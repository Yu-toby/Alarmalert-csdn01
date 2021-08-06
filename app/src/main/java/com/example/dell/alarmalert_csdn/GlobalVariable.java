package com.example.dell.alarmalert_csdn;

public class GlobalVariable {
    private Boolean isleave;

    public void NoticeIsleave(Boolean leave){
        this.isleave = leave;
    }
    public Boolean Isleave(){
        return isleave;
    }
}
