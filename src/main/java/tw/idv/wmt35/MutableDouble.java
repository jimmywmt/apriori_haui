package tw.idv.wmt35;

public class MutableDouble {
    private double value;

    public MutableDouble ( double value ) {
        this.value = value;
    }

    public double getValue () {
        return this.value;
    }

    public void setValue ( double value ) {
        this.value = value;
    }

    public String toString () {
        return (new Double(value)).toString();
    }
}
