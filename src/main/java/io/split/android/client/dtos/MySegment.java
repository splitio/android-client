package io.split.android.client.dtos;

public class MySegment {
    public String id;
    public String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MySegment mySegment = (MySegment) o;

        return name.equals(mySegment.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
