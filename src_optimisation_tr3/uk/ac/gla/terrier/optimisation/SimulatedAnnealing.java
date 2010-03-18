/*
 * Created on 20-Jun-2005
 */
package uk.ac.gla.terrier.optimisation;

import java.util.Random;

/**
 * @author vassilis
 */
public class SimulatedAnnealing {

	protected double[][] p;

	protected double y[];

	protected int ndim;

	protected double[] pb;

	protected double yb;

	protected double ftol;

	protected ManyVariableFunction funk;

	protected int iter;

	protected double temptr;

	public void setp(double[][] _p) {
		p = _p;
	}

	public double[][] getp() {
		return p;
	}

	public void sety(double[] _y) {
		y = _y;
	}

	public double[] gety() {
		return y;
	}

	public void setndim(int _ndim) {
		ndim = _ndim;
	}

	public int getndim() {
		return ndim;
	}

	public void setyb(double _yb) {
		yb = _yb;
	}

	public double getyb() {
		return yb;
	}

	public void setpb(double[] _pb) {
		pb = _pb;
	}

	public double[] getpb() {
		return pb;
	}

	public void setftol(double _ftol) {
		ftol = _ftol;
	}

	public double getftol() {
		return ftol;
	}

	public void setfunk(ManyVariableFunction _funk) {
		funk = _funk;
	}

	public ManyVariableFunction getfunk() {
		return funk;
	}

	public void setiter(int _iter) {
		iter = _iter;
	}

	public int getiter() {
		return iter;
	}

	public void settmptr(double _temptr) {
		temptr = _temptr;
	}

	public double gettemptr() {
		return temptr;
	}

	public double tt; // communication between amebsa and amotsa

	protected Random random = new Random();

	protected double yhi;

	public void amebsa() {
		int i, ihi, ilo, j, m, n;
		int mpts = ndim + 1;
		double rtol, sum, swap, ylo, ynhi, ysave, yt, ytry;
		double[] psum;

		yhi = 0.0d;

		psum = new double[ndim];
		tt = -temptr;

		//get_PSUM
		for (n = 0; n < ndim; n++) {
			for (sum = 0.0, m = 0; m < mpts; m++)
				sum += p[m][n];
			psum[n] = sum;
		}

		for (;;) {
			ilo = 0;
			ihi = 1;

			ynhi = ylo = y[0] + tt * Math.log(random.nextDouble())
					/ Math.log(2.0d);
			yhi = y[1] + tt * Math.log(random.nextDouble()) / Math.log(2.0d);

			if (ylo > yhi) {
				ihi = 0;
				ilo = 1;
				ynhi = yhi;
				yhi = ylo;
				ylo = ynhi;
			}
			for (i = 2; i < mpts; i++) {
				yt = y[i] + tt * Math.log(random.nextDouble()) / Math.log(2.0d);
				if (yt <= ylo) {
					ilo = i;
					ylo = yt;
				}
				if (yt > yhi) {
					ynhi = yhi;
					ihi = i;
					yhi = yt;
				} else if (yt > ynhi) {
					ynhi = yt;
				}
			}

			rtol = 2.0d * Math.abs(yhi - ylo) / (Math.abs(yhi) + Math.abs(ylo));
			if (rtol < ftol || iter < 0) {
				swap = y[0];
				y[0] = y[ilo];
				y[ilo] = swap;
				for (n = 0; n < ndim; n++) {
					swap = p[0][n];
					p[0][n] = p[ilo][n];
					p[ilo][n] = swap;
				}
				break;
			}
			iter -= 2;

			ytry = amotsa(p, y, psum, ndim, pb, /* yb, */funk, ihi, /* yhi, */
					-1.0d);
			if (ytry <= ylo) {
				ytry = amotsa(p, y, psum, ndim, pb, /* yb, */funk, ihi, /* yhi, */
						2.0);
			} else if (ytry >= ynhi) {
				ysave = yhi;
				ytry = amotsa(p, y, psum, ndim, pb, /* yb, */funk, ihi, /* yhi, */
						0.5d);
				if (ytry >= ysave) {
					for (i = 0; i < mpts; i++) {
						if (i != ilo) {
							for (j = 0; j < ndim; j++) {
								psum[j] = 0.5 * (p[i][j] + p[ilo][j]);
								p[i][j] = psum[j];
							}
							y[i] = funk.evaluate(psum);
						}
					}
					iter -= ndim;
					//get_PSUM
					for (n = 0; n < ndim; n++) {
						for (sum = 0.0, m = 0; m < mpts; m++)
							sum += p[m][n];
						psum[n] = sum;
					}
				}
			} else
				iter++;
		}
	}

	public double amotsa(double[][] _p, double[] _y, double[] _psum, int ndim,
			double[] _pb, /* double[] yb, */
			ManyVariableFunction _funk, int ihi, double fac) {
		int j;
		double fac1, fac2, yflu, ytry;
		double[] _ptry;

		_ptry = new double[ndim];
		fac1 = (1.0d - fac) / ndim;
		fac2 = fac1 - fac;
		for (j = 0; j < ndim; j++)
			_ptry[j] = _psum[j] * fac1 - _p[ihi][j] * fac2;
		ytry = _funk.evaluate(_ptry);
		if (ytry <= yb) {
			for (j = 0; j < ndim; j++)
				_pb[j] = _ptry[j];
			yb = ytry;
		}
		yflu = ytry - tt * Math.log(random.nextDouble()) / Math.log(2.0d);
		if (yflu < yhi) {
			_y[ihi] = ytry;
			yhi = yflu;
			for (j = 0; j < ndim; j++) {
				_psum[j] += _ptry[j] - _p[ihi][j];
				_p[ihi][j] = _ptry[j];
			}
		}
		return yflu;
	}

	/*public static void main(String[] args) {

		TerrierFunction terrierFunc = new TerrierFunction();

		SimulatedAnnealing sa = new SimulatedAnnealing();

		int ndim = 1;
		sa.setndim(ndim);
		SimulatedAnnealing.INIT_SIZE = Double.parseDouble(args[1]);
		SimulatedAnnealing.THRESHOLD = Double.parseDouble(args[2]);
		double[] x = new double[] { Double.parseDouble(args[0]) };
		sa.local_optimise(terrierFunc, x);
		System.out.println("maximum " + terrierFunc.evaluate(x[0]) + " at " + x[0]);

		terrierFunc.close();
	}*/

	public static double INIT_SIZE = 0.4;

	int count = 0;

	public static double THRESHOLD = 1E-4;

	public double[] local_optimise(ManyVariableFunction f, double[] x) {
		double[] u = new double[ndim];
		double[] v = new double[ndim];
		double[] xv = new double[ndim];

		double fx, fxv, vr;
		int vi, vvec, i, iter, maxiter;

		for (i = 0; i < ndim; i++) {
			u[i] = 0.0d;
			v[i] = 0;
		}

		vi = -1;
		vvec = 1;
		vr = -INIT_SIZE;
		fx = f.evaluate(x);
		fxv = 1E10;
		System.out.print(count + " " + fx + " <= ");
		for (i = 0; i < ndim; i++) {
			System.out.print(x[i] + " ");
		}
		;
		System.out.println();
		while (Math.abs(vr) >= THRESHOLD) {
			maxiter = ((Math.abs(vr) < 2 * THRESHOLD) ? 2 * ndim : 2);
			iter = 0;
			while ((fxv >= fx) && (iter < maxiter)) {
				if (iter == 0) {
					for (i = 0; i < ndim; i++)
						xv[i] = x[i];
				} else
					xv[vi] -= vr;

				if (vvec != 0)
					vvec = 0;
				vr = -vr;
				if (vr > 0)
					vi = ((vi + 1) % ndim);
				xv[vi] += vr;
				fxv = f.evaluate(xv);
				iter++;
			}
			if (fxv >= fx) {
				fxv = 1E10;
				vr /= 2;
			} else {
				fx = fxv;
				System.out.print(count + " " + fx + " <= ");
				for (i = 0; i < ndim; i++) {
					x[i] = xv[i];
					System.out.print(x[i] + " ");
				}
				System.out.print("\n");
				if (iter == 0) {
					if (vvec!=0) {
						for (i = 0; i < ndim; i++) {
							u[i] += v[i];
							v[i] *= 2;
							xv[i] += v[i];
						}
						vr *= 2;
					} else {
						u[vi] += vr;
						vr *= 2;
						xv[vi] += vr;
					}
					fxv = f.evaluate(xv);
				} else {
					for (i = 0; i < ndim; i++)
						xv[i] += u[i];
					xv[vi] += vr;
					fxv = f.evaluate(xv);
					if (fxv >= fx) {
						for (i = 0; i < ndim; i++) {
							u[i] = 0;
							xv[i] = x[i];
						}
						u[vi] = vr;
						vr *= 2;
						xv[vi] += vr;
						fxv = f.evaluate(xv);
					} else {
						for (i = 0; i < ndim; i++)
							x[i] = xv[i];
						fx = fxv;
						u[vi] += vr;
						for (i = 0; i < ndim; i++)
							v[i] = 2 * u[i];
						vvec = 1;
						for (i = 0; i < ndim; i++)
							xv[i] += v[i];
						fxv = f.evaluate(xv);
						for (vr = 0, i = 0; i < ndim; i++)
							vr += v[i] * v[i];
						vr = Math.sqrt(vr);
					}
				}
			}
		}
		return x;
	}
}
