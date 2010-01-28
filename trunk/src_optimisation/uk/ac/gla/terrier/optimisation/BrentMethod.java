package uk.ac.gla.terrier.optimisation;

/**
 * @author Vassilis Plachouras
 * 
 */
public class BrentMethod implements OneDimensionOptimiser {

	public static final int ITMAX = 100;
	public static final double CGOLD = 0.3819660;
	public static final double ZEPS = 1e-10;
	
	protected double ax;
	protected double bx;
	protected double cx;
	
	protected double xmin;
	
	protected OneVariableFunction f;
	
	protected double tol;

	public BrentMethod() { }
	
	public BrentMethod(double _ax, double _bx, double _cx, OneVariableFunction _f, double _tol) {
		ax = _ax;
		bx = _bx;
		cx = _cx;
		f = _f;
		tol = _tol;
	}
	
	public void setax(double _ax) { ax = _ax; }
	public void setbx(double _bx) { bx = _bx; }
	public void setcx(double _cx) { cx = _cx; }
	
	public void setFunction(OneVariableFunction _f) { f = _f; }
	
	public void setTol(double _tol) { tol = _tol; }
	
	public double getxmin() { return xmin; }

	
	public double optimise() {
		int iter;
		double a,b,d=0.0d,etemp,fu,fv,fw,fx,p,q,r,tol1,tol2;
		double u,v,w,x,xm;
		double e = 0.0;
		
		a = (ax < cx ? ax : cx);
		b = (ax > cx ? ax : cx);
		
		x = w = v = bx;
		
		fw = fv = fx = f.evaluate(x);
		
		for (iter = 1; iter <=ITMAX; iter++) {
			xm = 0.5d * (a + b);
			tol2 = 2.0d * (tol1=tol*Math.abs(x)+ZEPS);
			if (Math.abs(x - xm) <= (tol2-0.5*(b-a))) {
				xmin = x;
				return fx;
			}
			if (Math.abs(e) > tol1) {
				r = (x-w)*(fx-fv);
				q = (x-v)*(fx-fw);
				p = (x-v)*q-(x-w)*r;
				q = 2.0d * (q-r);
				if (q > 0.0d) p = -p;
				q = Math.abs(q);
				etemp = e;
				e = d;
				if (Math.abs(p) >= Math.abs(0.5d*q*etemp) || p <= q*(a-x) || p >=q*(b-x))
					d = CGOLD*(e=(x >= xm ? a-x : b-x));
				else {
					d = p/q;
					u = x + d;
					if (u-a <tol2 || b-u < tol2) {
						d = NRUtils.sign(tol1, xm - x);
					}
				}
			} else {
				d = CGOLD * (e=(x >= xm ? a-x : b-x));
			}
			u = (Math.abs(d) >= tol1 ? x+d : x+NRUtils.sign(tol1,d));
			fu = f.evaluate(u);
			if (fu <= fx) {
				if (u >= x) a = x; else b = x;
				v = w; w = x; x = u;
				fv = fw; fw = fx; fx = fu;
			} else {
				if (u < x) a = u; else b = u;
				if (fu <= fw || w == x) {
					v = w;
					w = u;
					fv = fw; 
					fw = fu;
				} else if (fu <= fv || v == x || v == w) {
					v = u;
					fv = fu;
				}
			}
		}
		System.err.println("too many iterations in brent");
		xmin = x;
		return fx;
	}

}
