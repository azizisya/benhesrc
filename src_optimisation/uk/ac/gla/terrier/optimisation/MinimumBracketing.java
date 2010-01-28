package uk.ac.gla.terrier.optimisation;

/**
 * Given a function func, and given distinct initial points ax and bx, 
 * this routine searches in the downhill direction (defined by the function 
 * as evaluated at the initial points) and returns new points ax, bx, cx 
 * that bracket a minimum of the function. Also returned are the 
 * function values at the three points, fa, fb, and fc.
 * 
 * Code adapted from numerical recipes for C
 *  
 * @author Vassilis Plachouras
 *
 */
public class MinimumBracketing {

	protected OneVariableFunction f; 
	
	public static final double GOLD = 1.618034;
	public static final double GLIMIT = 100.0;
	public static final double TINY  = 1.0e-20;
	
	protected double fa; 
	protected double fb;
	protected double fc;
	
	protected double ax;
	protected double bx;
	protected double cx;
	
	public MinimumBracketing() { }
	
	public MinimumBracketing(double _ax, double _bx, OneVariableFunction _f) {
		ax = _ax;
		bx = _bx;
		f = _f;
	}
	
	public void setxa(double _ax) { ax = _ax; }
	public void setxb(double _bx) { bx = _bx; }
	public void setxc(double _cx) { cx = _cx; }
	
	public double getxa() { return ax; }
	public double getxb() { return bx; }
	public double getxc() { return cx; }
	
	public double getfa() { return fa; }
	public double getfb() { return fb; }
	public double getfc() { return fc; }
		
	public void setFunction(OneVariableFunction _f) { f = _f; }
	
	public void mnbrak() {
		double ulim, u, r, q, fu, dum;
		
		fa= f.evaluate(ax);
		fb= f.evaluate(bx);

		//Switch roles of a and b so that we can go
		//downhill in the direction from a to b
		if (fb > fa) { 
			dum = ax; ax = bx; bx = dum;
		}
		
		cx = bx + GOLD*(bx-ax);
		fc = f.evaluate(cx);
		
		while (fb > fc) {
			r = (bx - ax) * (fb - fc);
			q = (bx - cx) * (fb - fa);
			u = bx - ((bx - cx) * q - (bx - ax) * r) 
				/ (2.0d * NRUtils.sign(NRUtils.fmax(Math.abs(q-r), TINY), q-r));
		
			ulim = bx + GLIMIT*(cx - bx);
			
			if ((bx - u) * (u - cx) > 0.0d) {
				fu = f.evaluate(u);
				if (fu < fc) {
					ax = bx;
					bx = u;
					fa = fb;
					fb = fu;
					return;
				} else if (fu > fb) {
					cx = u;
					fc = fu;
					return;
				}
				u = cx + GOLD * (cx - bx);
				fu = f.evaluate(u);
			} else if ( (cx - u)*(u - ulim) > 0.0d) {
				fu = f.evaluate(u);
				if (fu < fc) {
					bx = cx; cx = u; u = cx+GOLD*(cx - bx);
					fb = fc; fc = fu; fu = f.evaluate(u);
				}
			} else if ((u - ulim)*(ulim - cx) >= 0.0d) {
				u = ulim; 
				fu = f.evaluate(u);
			} else {
				u = cx + GOLD * (cx - bx);
				fu = f.evaluate(u);
			}
			ax = bx; bx = cx; cx = u;
			fa = fb; fb = fc; fc = fu;
		}
	}
	
	public static void main(String[] args) {
		
		OneVariableFunction f = new OneVariableFunction() {
			public double evaluate(double p) {
				return Math.cos(p);
			}
		};
		
		System.out.println("f(0.5)= " + f.evaluate(0.5d));
		System.out.println("f(0.0)= " + f.evaluate(0.0d));
		System.out.println("f(1.13)= " + f.evaluate(1.13d));
		System.out.println("f(0.8)= " + f.evaluate(0.8d));
		System.out.println("f(pi)= " + f.evaluate(Math.PI));
		
		MinimumBracketing mbracket = new MinimumBracketing(0.5d, 1.13d, f);
		
		mbracket.setFunction(f);
		mbracket.mnbrak();

		System.out.println("xa = " + mbracket.getxa());
		System.out.println("xb = " + mbracket.getxb());
		System.out.println("xc = " + mbracket.getxc());
		
		System.out.println("fa = " + mbracket.getfa());
		System.out.println("fb = " + mbracket.getfb());
		System.out.println("fc = " + mbracket.getfc());
	}
}
