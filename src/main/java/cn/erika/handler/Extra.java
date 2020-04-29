package cn.erika.handler;

import cn.erika.core.Attribute;

public enum Extra implements Attribute {
    ENCRYPT(0x100),
    PUBLIC_KEY(0x101),
    PASSWORD(0x102),
    NICKNAME(0x200),
    HIDE(0x201);

    int value;

    Extra(int value) {
        this.value = value;
    }
}