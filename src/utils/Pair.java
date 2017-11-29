package utils;

public class Pair implements java.io.Serializable {
    private float curr;
    private float fut;

    public Pair(float curr, float fut) {
        this.curr = curr;
        this.fut = fut;
    }

    public float getCurr() {
        return curr;
    }

    public float getFut() {
        return fut;
    }
}
