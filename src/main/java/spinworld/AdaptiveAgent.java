package spinworld;

import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public class AdaptiveAgent extends SpinWorldAgent {

	double prevUtility = 0;

	public AdaptiveAgent(UUID id, String name, Location myLocation, int velocity, double radius, 
			double a, double b, double c, double pCheat, double alpha, double beta, Cheat cheatOn, 
			NetworkLeaveAlgorithm netLeave, boolean resetSatisfaction, long rndSeed, double t1, 
			double t2, double theta, double phi) {
		super(id, name, myLocation, velocity, radius, a, b, c, pCheat, 
				alpha, beta, cheatOn, netLeave, resetSatisfaction, rndSeed, 
				t1, t2, theta, phi);
	}

	@Override
	protected boolean chooseStrategy() {
		// Choose strategies for each round				
		double currentUtility = this.rollingUtility.getMean();					
		
		double benefit = currentUtility - prevUtility;
			
		// If you benefited from being compliant or lost out due to non-compliance, be more compliant
		// Else, try your chance against the reinforcement algorithm
		if ((benefit > 0 && this.compliantRound) || (benefit < 0 && !this.compliantRound))
			this.pCheat = this.pCheat - phi * this.pCheat;
		else if (benefit < 0 && this.compliantRound)
			reinforcementToCheat(theta);
		else
			reinforcementToCheat(benefit);
			
		// Update previous round's utility
		this.prevUtility = currentUtility;
		
		return (rnd.nextDouble() >= pCheat);
	}

	private void reinforcementToCheat(double benefit) {
		this.risk = this.resourcesGame.getObservedRiskRate(getID());
		this.catchRate = this.resourcesGame.getObservedCatchRate(getID());
		
		double reinforcement = 0.0;
		if ((benefit + risk + catchRate) != 0.0) {
			double normBenefit = benefit/(benefit + risk + catchRate);
			double normRisk = risk/(benefit + risk + catchRate);
			double normCatchRate = catchRate/(benefit + risk + catchRate);
			reinforcement = theta * (normBenefit - normRisk - normCatchRate);
		}
		
		if (reinforcement > 0.0)
			this.pCheat = this.pCheat + reinforcement * (1 - pCheat);
		else
			this.pCheat = this.pCheat + reinforcement * pCheat;
	}

}