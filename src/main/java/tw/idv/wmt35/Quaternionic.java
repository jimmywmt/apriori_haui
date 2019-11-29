package tw.idv.wmt35;

/**
 * Created by wumingtai on 2019/02/24.
 */
public class Quaternionic<W, X, Y, Z> {
    public W first;
    public X second;
    public Y third;
    public Z fourth;

    public Quaternionic (W first, X second, Y third, Z fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public String toString () {
        return ((new StringBuilder("(").append(first).append(", ").append(second).append(", ").append(third).append(", ").append(fourth).append(")")).toString());
    }
}
