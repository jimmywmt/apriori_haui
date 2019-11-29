package tw.idv.wmt35;

/**
 * Created by wumingtai on 2017/11/13.
 */
public class Pair<F, S> {
    public F first;
    public S second;

    public Pair ( F first, S second ) {
        this.first = first;
        this.second = second;
    }

    public String toString () {
        return ((new StringBuilder("(").append(first).append(", ").append(second).append(")")).toString());
    }
}
