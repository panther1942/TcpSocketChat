package cn.erika.core;

/**
 * 用于设置Socket的属性
 * 核心属性0x00-0xFF
 */
public interface Attribute {
    enum Standard implements Attribute {
        CACHE_SIZE(0x0);

        int value;

        Standard(int value) {
            this.value = value;
        }
    }
}
