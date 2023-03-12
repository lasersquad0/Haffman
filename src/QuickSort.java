public class QuickSort
{
	private static final int CUTOFF =  100;   // cutoff to insertion sort
	public static int maxDDepth = 0;
	public static int maxRecursion = 0;

	//https://algs4.cs.princeton.edu/code/edu/princeton/cs/algs4/Quick3string.java.html
	public static void bwtsort(byte[] a, int[] ind, int sz, int lo, int hi, int d, int r)
	{
		maxDDepth = Math.max(maxDDepth, d);
		maxRecursion = Math.max(maxRecursion, r);

		// cutoff to insertion sort for small subarrays
		if (hi <= lo + CUTOFF) {
			//bwtbuble(a, ind, sz, lo, hi, d);
			bwtinsertion(a, ind, sz, lo, hi, d);
			return;
		}

		int lt = lo, gt = hi;
		int v = digb(a, sz, ind[lo], d);
		int i = lo + 1;
		while (i <= gt) {
			int t = digb(a, sz, ind[i], d);
			if      (t < v) exch(a, ind, lt++, i++);
			else if (t > v) exch(a, ind, i, gt--);
			else              i++;
		}

		// a[lo..lt-1] < v = a[lt..gt] < a[gt+1..hi].
		bwtsort(a, ind, sz, lo, lt-1, d, r+1);
		if(d < sz)/*(v >= 0)*/ bwtsort(a, ind, sz, lt, gt, d+1, r+1);
		bwtsort(a, ind, sz,gt+1, hi, d, r+1);
	}

	private static void bwtbuble(byte[] a, int[] ind, int sz, int lo, int hi, int d)
	{
		for (int i = lo; i < hi; i++)
			for (int j = i+1; j <= hi; j++)
			{
				if(!bwtless(a, sz, ind[i], ind[j], d))
					exch(a, ind, i, j);
			}
	}
	// sort from a[lo] to a[hi], starting at the dth character
	private static void bwtinsertion(byte[] a, int[] ind, int sz, int lo, int hi, int d)
	{
		for (int i = lo; i <= hi; i++)
			for (int j = i; (j > lo) && bwtless(a,sz,ind[j],ind[j-1],d); j--)
				exch(a, ind, j, j-1);
	}
	private static int digb(byte[] v, int sz, int iorig, int d)
	{
		return Byte.toUnsignedInt(v[(d+iorig) % sz]);
	}

	private static void exch(byte[] a, int[] ind, int i, int j)
	{
	//	exchC++;
		int tt = ind[i];
		ind[i] = ind[j];
		ind[j] = tt;
	}

	private static boolean bwtless(byte[] v, int sz, int iorig, int jorig, int d)
	{
		//assert v.substring(0, d).equals(w.substring(0, d));
		for (int i = d; i < sz; i++)
		{
			int b1 = digb(v, sz, iorig, i);
			int b2 = digb(v, sz, jorig, i);
			if (b1 < b2) return true;
			if (b1 > b2) return false;
		}
		return false;
	}

	public static boolean isSorted(StringBuilder[] a)
	{
		for (int i = 1; i < a.length; i++)
			if (a[i].compareTo(a[i-1]) < 0) return false;
		return true;
	}

	/*

	// is v less than w, starting at character d
	private static boolean less(StringBuilder v, StringBuilder w, int d)
	{
		assert v.substring(0, d).equals(w.substring(0, d));
		for (int i = d; i < Math.min(v.length(), w.length()); i++)
		{
			if (v.charAt(i) < w.charAt(i)) return true;
			if (v.charAt(i) > w.charAt(i)) return false;
		}
		return v.length() < w.length();
	}


	private static void quickSortX(StringBuilder[] a, int l, int r, int d)
	{
		int i = l - 1, j = r, p = l - 1, q = r, k;
		int v;
		v = dig(a[r], d);
		while(i < j)
		{
			while(dig(a[++i],d) < v);
			while (v < dig(a[--j],d)) if (j == l) break;
			if(i > j) break;
			excha(a, i, j);
			if(dig(a[i],d) == v) {p++; excha(a, p, i);}
			if(dig(a[j],d) == v) {q--; excha(a, j, q);}
		}

		if(p == q)
		{
			//if(v !="\0")
			if(d < a[r].length()) quickSortX(a, l, r, d+1);
			return;
		}

		if(dig(a[i],d) < v) i++;
		for(k = l; k <= p; k++, j--) excha(a, k, j);
		for(k = r; k >= q; k--, i++) excha(a, k, i);
		quickSortX(a, l, j, d);
		if( (i == r) && (dig(a[i],d) == v)) i++;
		//if(v == "\0")
		if(d < a[r].length()) quickSortX(a, j+1, i-1, d+1);
		quickSortX(a, i, r, d);
	}


 */
}
