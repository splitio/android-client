package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

public class MySegment {
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;

    public static MySegment create(String name) {
        MySegment mySegment = new MySegment();
        mySegment.name = name;
        return mySegment;
    }

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
