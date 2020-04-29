package cn.erika.core;

public interface Attribute {
    enum Standard implements Attribute {
        CACHE_SIZE(0x0);

        int value;

        Standard(int value) {
            this.value = value;
        }
    }
}
