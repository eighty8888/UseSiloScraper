import java.time.Duration;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

public class SiloScraper {
	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		WebDriverManager.chromedriver().setup();
		WebDriver driver = new ChromeDriver();
		
		// get info
		System.out.println("Enter username: ");
		String userName = input.nextLine();
		System.out.println("Enter password: ");
		String passWord = input.nextLine();
		driver.get("https://app.usesilo.com/login");
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
		Actions actions = new Actions(driver);
		
		//get dates
		System.out.println("Enter starting date: ");
		String startingDate = input.nextLine();
		System.out.println("Enter ending date: ");
		String endingDate = input.nextLine();
		navigateToSite(driver, actions, userName, passWord, startingDate, endingDate);
		
		//choose option
		String menu = "Menu: \n"
				+ "S: Scrape all orders and lots\n"
				+ "C: Close all 0 O/H lots\n"
				+ "Q: Quit";
		System.out.println(menu);
		System.out.println("Choose an option: ");
		String answer = input.nextLine();
		boolean keepGoing = true;
		while (keepGoing) {
			switch (answer) {
				case "S": 
					try {
						scrapeSilo(driver, actions);
						System.out.println("Successfully scraped the page.");
						refreshPage(driver, actions);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;
				case "C": 
					try {
						closeZeroLots(driver, actions);
						System.out.println("All 0 on hand lots have been closed.");
						refreshPage(driver, actions);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;
				case "Q": 
					driver.quit();
					keepGoing = false;
					break;
				default: 
					System.out.println("Invalid input");
			}
			if (keepGoing) {
				System.out.println();
				System.out.println(menu);
				System.out.println("Choose an option: ");
				answer = input.nextLine();
			}
		}
		input.close();
	}
	
	/**
	 * Navigates through the login page, buyer page, and stops at the lots tab within the seller page.
	 * @param driver
	 *    The WebDriver object needed to run the browser.
	 * @param actions
	 *    An Actions object used to implement keyboard actions.
	 * @param userName
	 *    The username to be entered for login
	 * @param passWord
	 *    The password to be entered for login
	 * @param startingDate
	 *    The start date of the period to navigate to
	 * @param endingDate
	 *    The end date of the period to navigate to
	 * @throws InterruptedException 
	 *    Indicates an interruption.
	 * @custom.postconditions
	 *    Selenium will have found its way to the lots page.
	 */
	public static void navigateToSite(WebDriver driver, Actions actions, String userName, String passWord, String startingDate, String endingDate) {
		//input user and pw and click login
		driver.findElement(By.xpath("//input [@name = 'email' and @class = 'Input-thbaps-2 iKAaHU']")).sendKeys(userName);
		driver.findElement(By.xpath("//input [@name = 'password' and @class = 'Input-thbaps-2 iKAaHU']")).sendKeys(passWord);
		WebElement clickLogin = driver.findElement(By.xpath("//button [@class ='Container-sc-3l6nxo-0 fzbIcn BrandButton-sc-294yet-0 KaleBrandButton-sc-294yet-1 FullWidthKaleButton-sc-294yet-5 bmCVvA dTOAMT fvdLAx']"));
		actions.click(clickLogin).perform();
		
		//go to seller
		WebElement goToSeller = new WebDriverWait(driver, Duration.ofSeconds(10))
			.until(ExpectedConditions.elementToBeClickable(By.xpath("//a [@href = '/seller']")));
		actions.click(goToSeller).perform();
		
		//go to lots
		WebElement goToLots = new WebDriverWait(driver, Duration.ofSeconds(10))
			.until(ExpectedConditions.elementToBeClickable(By.xpath("//a [@href = '/seller/lots']")));
		actions.click(goToLots).perform();
		
		//go to calendar and enter the dates
		WebElement startDate = driver.findElement(By.xpath("//input [@placeholder = 'Start Date']"));
		actions.click(startDate)
		.keyDown(Keys.CONTROL)
		.sendKeys("a")
		.keyUp(Keys.CONTROL)
		.sendKeys(startingDate)
		.perform();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
		WebElement endDate = driver.findElement(By.xpath("//input [@placeholder = 'End Date']"));
		actions.click(endDate)
		.keyDown(Keys.CONTROL)
		.sendKeys("a")
		.keyUp(Keys.CONTROL)
		.sendKeys(endingDate)
		.perform();
		WebElement backOut = new WebDriverWait(driver, Duration.ofSeconds(10))
			.until(ExpectedConditions.elementToBeClickable(By.xpath("//div [@type = 'Order']")));
		actions
		.click(backOut)
		.perform();
	}
	
	/**
	 * Scrapes the entire viewable page for all silo orders and every lot within each order. Then the page will scroll down 
	 * and repeat the process, storing every silo order within a hashtable. Finally, this method will print the entire hashtable in 
	 * descending order numerically based on the PO Number of each silo order.
	 * @param driver
	 *    The WebDriver object needed to run the browser.
	 * @param actions
	 *    An Actions object used to implement keyboard actions.
	 * @custom.postconditions
	 *    Prints a neatly formatted table of every order and their lots.
	 * @throws InterruptedException
	 *    Indicates an interruption.
	 */
	public static void scrapeSilo(WebDriver driver, Actions actions) throws InterruptedException {
		Hashtable<Integer, SiloOrder> allOrders = new Hashtable<>();
		//oldHeight: the original pixel height of the page
		int oldHeight = driver.findElement(By.xpath("(//div [contains(@style, 'height:')])[4]")).getRect().getHeight();
		boolean keepScrolling = true;
		//sameHeightMax: the max amount of times that the method will scroll before stopping since the end of the page has been reached.
		int sameHeightMax = 3;
		//sameHeightMet: the amount of times that scrolling hasn't actually changed the size of the page.
		int sameHeightMet = 0;
		while (keepScrolling) {
			//get po number
			ArrayList<Integer> allPONumbers = new ArrayList<>();
			ArrayList<WebElement> allPONumbersElem = (ArrayList<WebElement>) driver
					.findElements(By.xpath("//button [contains(@data-interactive, 'po-number') and contains(@data-interactive, '-link')]//span [@class = 'chakra-text css-15s788t']"));
			//continueCounter: this counter is used to determine how many elements have been already counted due to the inconsistency of the orders' sizes relative to the current page. 
			//So, when the findelements() method pulls all the elements currently on the page, if any of those elements have already been parsed and put into the hashtable, the 
			//continueCounter will prevent any accidental duplicates or recounting of those elements after scrolling.
			int continueCounter = 0;
			for (WebElement x : allPONumbersElem) {
				int POKey = Integer.parseInt(x.getText());
				if (allOrders.containsKey(POKey)) {
					continueCounter++;
					continue;
				}
				allPONumbers.add(POKey);
			}
		
			//get vendor
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
			ArrayList<String> allVendorNames = new ArrayList<>();
			ArrayList<WebElement> allVendorNamesElem = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'ColumnsWrapper-sc-77li46-0 hMDtDP']//div[4] [@class = 'TableCell-y4vxke-0 gZWkak']"));
			//passedElements: this counter is used to skip the number of duplicate elements counted by continueCounter, so that while findElements() will capture all elements on the page
			//the parsed ArrayList will not include any duplicates and its size and contents will stay consistent with the unique PONumbers.
			int passedElements = 0;
			for (WebElement x : allVendorNamesElem) {
				if (continueCounter > 0 && passedElements != continueCounter) {
					passedElements++;
					continue;
				}
				String vendor = x.getText();
				if (vendor.contains("Vendor")) {
					vendor = vendor.substring(7);
				}
				allVendorNames.add(vendor);
			}
			
			//get receive date
			ArrayList<String> allReceiveDates = new ArrayList<>();
			ArrayList<WebElement> allReceiveDatesElem = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'ColumnsWrapper-sc-77li46-0 hMDtDP']//div[5] [@class = 'TableCell-y4vxke-0 fbPTUH']"));
			passedElements = 0;
			for (WebElement x : allReceiveDatesElem) {
				if (continueCounter > 0 && passedElements != continueCounter) {
					passedElements++;
					continue;
				}
				String date = x.getText();
				if (date.contains("Received")) {
					date = date.substring(9);
				}
				allReceiveDates.add(date);
			}
			
			//get num of units
			ArrayList<Integer> allNumOfUnits = new ArrayList<>();
			ArrayList<WebElement> allNumOfUnitsElem = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'ColumnsWrapper-sc-77li46-0 hMDtDP']//div[6] [@class = 'TableCell-y4vxke-0 enrqvS']"));
			passedElements = 0;
			for (WebElement x : allNumOfUnitsElem) {
				if (continueCounter > 0 && passedElements != continueCounter) {
					passedElements++;
					continue;
				}
				String numUnits = x.getText();
				if (numUnits.contains("Inv. Units")) {
					numUnits = numUnits.substring(11);
				}
				allNumOfUnits.add(Integer.parseInt(numUnits));
			}
			
			//go to each tableexpansion per silo order
			ArrayList<WebElement> allLotTableExpansions = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'TableRowExpansion-sc-1ynybny-1 dgXkno']"));
			passedElements = 0;
			//start: this counter is used for referencing the correct index of the other ArrayLists of parsed data per order after duplicates have been accounted for.
			int start = 0;
			for (int i = 0; i < allLotTableExpansions.size(); i++) {
				if (continueCounter > 0 && passedElements != continueCounter) {
					passedElements++;
					continue;
				}
				WebElement x = allLotTableExpansions.get(i);
				ArrayList<SiloLot> lots = new ArrayList<>();
				
				//get lot numbers
				ArrayList<String> allLotNumbers = new ArrayList<>();
				ArrayList<WebElement> allLotNumbersElem = (ArrayList<WebElement>)x
						.findElements(By.xpath(".//div[@class = 'ColumnsWrapper-sc-77li46-0 eedQqe']/div[1] [@class = 'TableCell-y4vxke-0 fbPTUH']"));
				for (WebElement y : allLotNumbersElem) {
					String lotNumber = y.getText();
					allLotNumbers.add(lotNumber);
				}
				
				//get productInfo
				ArrayList<String> allLotInfos = new ArrayList<>();
				ArrayList<WebElement> allLotInfosElem = (ArrayList<WebElement>)x
						.findElements(By.xpath(".//div[2] [@class = 'TableCell-y4vxke-0 gZWkak']"));
				for (WebElement y : allLotInfosElem) {
					String lotInfo = y.getText().replaceAll("\n", " ");
					allLotInfos.add(lotInfo);
				}
				
				//get total units per lot
				ArrayList<Integer> allLotUnits = new ArrayList<>();
				ArrayList<WebElement> allLotUnitsElem = (ArrayList<WebElement>)x
						.findElements(By.xpath(".//div[3] [@class = 'TableCell-y4vxke-0 enrqvS']"));
				for (WebElement y : allLotUnitsElem) {
					int lotUnit = Integer.parseInt(y.getText());
					allLotUnits.add(lotUnit);
				}
				
				//get onHand units per lot
				ArrayList<Integer> allOnHands = new ArrayList<>();
				ArrayList<WebElement> allOnHandsElem = (ArrayList<WebElement>)x
						.findElements(By.xpath(".//div[4] [@class = 'TableCell-y4vxke-0 enrqvS']"));
				for (WebElement y : allOnHandsElem) {
					int onHand = Integer.parseInt(y.getText());
					allOnHands.add(onHand);
				}
				
				//get Open status per lot
				ArrayList<Boolean> allOpenStatuses = new ArrayList<>();
				ArrayList<WebElement> allOpenStatusesElem = (ArrayList<WebElement>)x
						.findElements(By.xpath(".//div [@class = 'TableCell-y4vxke-0 hhNCOs']"));
				for (WebElement y : allOpenStatusesElem) {
					if (y.getText().equals("Open")) {
						allOpenStatuses.add(false);
					}
					else if (y.getText().equals("Close")) {
						allOpenStatuses.add(true);
					}
				}
				for (int j = 0; j < allLotNumbers.size(); j++) {
					SiloLot newLot = new SiloLot(allLotNumbers.get(j), allLotInfos.get(j), allLotUnits.get(j), allOnHands.get(j), allOpenStatuses.get(j));
					lots.add(newLot);
				}
				SiloOrder newOrder = new SiloOrder(allVendorNames.get(start), allPONumbers.get(start), allReceiveDates.get(start), allNumOfUnits.get(start), lots);
				allOrders.put(newOrder.getPONumber(), newOrder);
				start++;
			}
			
			//scroll
			actions.sendKeys(Keys.PAGE_DOWN).perform();
			Thread.sleep(1700);
			//newHeight: the new pixel height of the page after scrolling
			int newHeight = driver.findElement(By.xpath("(//div [contains(@style, 'height:')])[4]")).getRect().getHeight();
			if (sameHeightMet == sameHeightMax) {
				break;
			}
			if (oldHeight == newHeight) {
				sameHeightMet++;
			}
			else {
				oldHeight = newHeight;
				sameHeightMet = 0;
			}
		}
		//organize hashtable
		Iterator<Integer> hashKeys = allOrders.keySet().iterator();
		ArrayList<Integer> keysList = new ArrayList<>();
		while (hashKeys.hasNext()) {
			Integer key = (Integer)hashKeys.next();
			keysList.add(key);
		}
		Collections.sort(keysList, Collections.reverseOrder());
		
		//print table
		for (Integer key : keysList) {
			System.out.println(allOrders.get(key).toString());
		}
		System.out.println("There are " + allOrders.size() + " total orders");
	}
	
	/**
	 * Checks every lot and clicks close on any open lots with 0 on hand products.
	 * @param driver
	 *    The WebDriver object needed to run the browser.
	 * @param actions
	 *    An Actions object used to implement keyboard actions.
	 * @throws InterruptedException 
	 *    Indicates an interruption.
	 * @custom.postconditions
	 *    Every lot with 0 on hand products have been closed.
	 */
	public static void closeZeroLots(WebDriver driver, Actions actions) throws InterruptedException {
		Hashtable<String, SiloLot> allLots = new Hashtable<>();
		//oldHeight: the original pixel height of the page
		int oldHeight = driver.findElement(By.xpath("(//div [contains(@style, 'height:')])[4]")).getRect().getHeight();
		boolean keepScrolling = true;
		int sameHeightMax = 3;
		int sameHeightMet = 0;
		while (keepScrolling) {
			//continueCounter: this counter is used to determine how many elements have been already counted due to the inconsistency of the orders' sizes relative to the current page. 
			//So, when the findelements() method pulls all the elements currently on the page, if any of those elements have already been parsed and put into the hashtable, the 
			//continueCounter will prevent any accidental duplicates or recounting of those elements after scrolling.
			int continueCounter = 0;
			//get lot numbers
			ArrayList<String> allLotNumbers = new ArrayList<>();
			ArrayList<WebElement> allLotNumbersElem = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'TableRowExpansion-sc-1ynybny-1 dgXkno']//div[@class = 'ColumnsWrapper-sc-77li46-0 eedQqe']/div[1] [@class = 'TableCell-y4vxke-0 fbPTUH']"));
			for (WebElement y : allLotNumbersElem) {
				String lotNumber = y.getText();
				if (allLots.containsKey(lotNumber)) {
					continueCounter++;
					continue;
				}
				allLotNumbers.add(lotNumber);
			}
			
			//get productInfo
			ArrayList<String> allLotInfos = new ArrayList<>();
			ArrayList<WebElement> allLotInfosElem = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'TableRowExpansion-sc-1ynybny-1 dgXkno']//div[2] [@class = 'TableCell-y4vxke-0 gZWkak']"));
			//passedElements: this counter is used to skip the number of duplicate elements counted by continueCounter, so that while findElements() will capture all elements on the page
			//the parsed ArrayList will not include any duplicates and its size and contents will stay consistent with the unique PONumbers.
			int passedElements = 0;
			for (WebElement y : allLotInfosElem) {
				if (continueCounter > 0 && passedElements != continueCounter) {
					passedElements++;
					continue;
				}
				String lotInfo = y.getText().replaceAll("\n", " ");
				allLotInfos.add(lotInfo);
			}
			
			//get total units per lot
			ArrayList<Integer> allLotUnits = new ArrayList<>();
			ArrayList<WebElement> allLotUnitsElem = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'TableRowExpansion-sc-1ynybny-1 dgXkno']//div[3] [@class = 'TableCell-y4vxke-0 enrqvS']"));
			passedElements = 0;
			for (WebElement y : allLotUnitsElem) {
				if (continueCounter > 0 && passedElements != continueCounter) {
					passedElements++;
					continue;
				}
				int lotUnit = Integer.parseInt(y.getText());
				allLotUnits.add(lotUnit);
			}
			
			//get onHand units per lot
			ArrayList<Integer> allOnHands = new ArrayList<>();
			ArrayList<WebElement> allOnHandsElem = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'TableRowExpansion-sc-1ynybny-1 dgXkno']//div[4] [@class = 'TableCell-y4vxke-0 enrqvS']"));
			passedElements = 0;
			for (WebElement y : allOnHandsElem) {
				if (continueCounter > 0 && passedElements != continueCounter) {
					passedElements++;
					continue;
				}
				int onHand = Integer.parseInt(y.getText());
				allOnHands.add(onHand);
			}
			
			//get Open status per lot
			ArrayList<Boolean> allOpenStatuses = new ArrayList<>();
			ArrayList<WebElement> allOpenStatusesElem = (ArrayList<WebElement>)driver
					.findElements(By.xpath("//div [@class = 'TableRowExpansion-sc-1ynybny-1 dgXkno']//div [@class = 'TableCell-y4vxke-0 hhNCOs']"));
			passedElements = 0;
			//start: this counter is used for referencing the correct index of the other ArrayLists of parsed data per order after duplicates have been accounted for.
			int start = 0;
			for (WebElement y : allOpenStatusesElem) {
				if (continueCounter > 0 && passedElements != continueCounter) {
					passedElements++;
					continue;
				}
				if (y.getText().equals("Open")) {
					allOpenStatuses.add(false);
				}
				else if (y.getText().equals("Close")) {
					allOpenStatuses.add(true);
					if (allOnHands.get(start) == 0) {
						actions.click(y.findElement(By.xpath(".//div [@class = 'NoPrintWrapper-pz1agi-1 eVdeQm']"))).perform();
						WebElement okContButton = new WebDriverWait(driver, Duration.ofSeconds(10))
								.until(ExpectedConditions.elementToBeClickable(By.xpath("//div [@class = 'ConfirmationModal-sc-12dkxti-0 ivAaNv']//button [@data-interactive = 'continue-button']")));
						actions.click(okContButton).perform();
					}
				}
				start++;
			}
			for (int i = 0; i < allLotNumbers.size(); i++) {
				SiloLot newLot = new SiloLot(allLotNumbers.get(i), allLotInfos.get(i), allLotUnits.get(i), allOnHands.get(i), allOpenStatuses.get(i));
				allLots.put(newLot.getLotNumber(), newLot);
			}
			
			//scroll
			actions.sendKeys(Keys.PAGE_DOWN).perform();
			Thread.sleep(1700);
			//newHeight: the new pixel height of the page after scrolling
			int newHeight = driver.findElement(By.xpath("(//div [contains(@style, 'height:')])[4]")).getRect().getHeight();
			if (sameHeightMet == sameHeightMax) {
				break;
			}
			if (oldHeight == newHeight) {
				sameHeightMet++;
			}
			else {
				oldHeight = newHeight;
				sameHeightMet = 0;
			}
		}
	}
	
	/**
	 * Refreshes the page for another option to properly work.
	 * @param driver
	 *    The WebDriver object needed to run the browser.
	 * @param actions
	 *    An Actions object used to implement keyboard actions.
	 * @custom.postconditions
	 *    This page has been refreshed
	 */
	public static void refreshPage(WebDriver driver, Actions actions) {
		driver.navigate().refresh();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		WebElement backOut = new WebDriverWait(driver, Duration.ofSeconds(10))
				.until(ExpectedConditions.elementToBeClickable(By.xpath("//div [@type = 'Order']")));
		actions
		.click(backOut)
		.perform();
	}
}
