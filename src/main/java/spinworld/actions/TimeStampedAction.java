package spinworld.actions;

import uk.ac.imperial.presage2.core.Action;

public abstract class TimeStampedAction implements Action {

	int t;

	TimeStampedAction() {
		super();
	}

	TimeStampedAction(int t) {
		super();
		this.t = t;
	}

	// Get round number t
	public int getT() {
		return t;
	}

	// Set round number t
	public void setT(int t) {
		this.t = t;
	}

}

