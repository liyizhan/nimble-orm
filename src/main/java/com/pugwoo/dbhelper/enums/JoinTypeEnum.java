package com.pugwoo.dbhelper.enums;

/**
 * 两表join的方式枚举
 * @author pugwoo
 */
public enum JoinTypeEnum {

	JOIN("join", "default join"),
	LEFT_JOIN("left join", "left join"),
	RIGHT_JOIN("right join", "right join");
	
	private String code;
	
	private String name;
	
	private JoinTypeEnum(String code, String name) {
		this.code = code;
		this.name = name;
	}
	
	public static JoinTypeEnum getByCode(String code) {
		for(JoinTypeEnum e : JoinTypeEnum.values()) {
			if(code == e.getCode() || code != null && code.equals(e.getCode())) {
				return e;
			}
		}
		return null;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
}