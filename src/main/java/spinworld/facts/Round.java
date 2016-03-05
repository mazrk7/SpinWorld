package spinworld.facts;

import spinworld.RoundType;

// Round of the SpinWorld simulation
public class Round {

	public final int number;
	public final RoundType type;

	public Round(int number, RoundType type) {
		super();
		this.number = number;
		this.type = type;
	}
	
	@Override
	public String toString() {
		return "Round [number=" + number + ", type=" + type + "]";
	}

	public int getNumber() {
		return number;
	}

	public RoundType getType() {
		return type;
	}
	
	@Override
	public int hashCode() {
		return number;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Round other = (Round) obj;
		if (number != other.number)
			return false;
		return true;
	}

}

