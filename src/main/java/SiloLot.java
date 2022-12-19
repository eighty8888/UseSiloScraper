public class SiloLot {
	private String lotNumber;
	private String productInfo;
	private int total;
	private int onHand;
	private boolean open;
	
	public SiloLot() {
	}
	
	public SiloLot(String lotNumber, String productInfo, int total, int onHand, boolean open) {
		this.lotNumber = lotNumber;
		this.productInfo = productInfo;
		this.total = total;
		this.onHand = onHand;
		this.open = open;
	}
	
	public String getLotNumber() {
		return lotNumber;
	}
	public String getProductInfo() {
		return productInfo;
	}
	public int getTotal() {
		return total;
	}
	public int getOnHand() {
		return onHand;
	}
	public boolean getOpen() {
		return open;
	}
	public String toString() {
		String status = "OPEN";
		if (!open) {
			status = "CLOSED";
		}
		String table = String.format("%-16s | %-55s | %-14d | %-13d | %-6s ", lotNumber, productInfo, total, onHand, status);
		return table;
	}
}
