import java.util.*;

public class SiloOrder {
	private String vendor;
	private int PONumber;
	private String receiveDate;
	private int numUnits;
	private ArrayList<SiloLot> lots = new ArrayList<>();
	
	public SiloOrder() {
	}
	
	public SiloOrder(String vendor, int PONumber, String receiveDate, int numUnits, ArrayList<SiloLot> lots) {
		this.vendor = vendor;
		this.PONumber = PONumber;
		this.receiveDate = receiveDate;
		this.numUnits = numUnits;
		this.lots = lots;
	}
	
	public String getVendor() {
		return vendor;
	}
	public int getPONumber() {
		return PONumber;
	}
	public String getReceiveDate() {
		return receiveDate;
	}
	public int getNumUnits() {
		return numUnits;
	}
	public ArrayList<SiloLot> getLots() {
		return lots;
	}
	
	public void removeClosed() {
		for (SiloLot lot : lots) {
			if (!lot.getOpen()) {
				lots.remove(lot);
			}
		}
	}
	
	public void addLot(SiloLot lot) {
		this.lots.add(lot);
	}
	
	public String toString() {
		String table = String.format("PO #: %-7d | Vendor: %-28s | Received: %-10s | Inv. Units: %-6d \n"
				+ "===================================================================================================================\n"
				+ "Lot #: %-9s | Product: %-46s | Total:         | O/H:          | Lot Status: \n"
				+ "===================================================================================================================\n", PONumber, vendor, receiveDate, numUnits, "", "");
		for (SiloLot lot : lots) {
			table += lot.toString() + "\n";
		}
		table += "===================================================================================================================\n";
		return table;
	}
}
