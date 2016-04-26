package spinworld;

// Graduated levels of sanctioning depending on the crime
public enum GraduationLevel {
	
	NO_SANCTION(0), 
	WARNING(1), 
	EXPULSION(2);
	
	private int value;
	     
	private GraduationLevel(int value) {
	    this.value = value;
	}

	public int getValue() {
		return this.value;
	}

}
