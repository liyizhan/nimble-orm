package com.pugwoo.dbhelper.test.vo;

import com.pugwoo.dbhelper.annotation.*;
import com.pugwoo.dbhelper.enums.JoinTypeEnum;
import com.pugwoo.dbhelper.test.entity.SchoolDO;
import com.pugwoo.dbhelper.test.entity.StudentDO;
import lombok.Data;

@Data
@JoinTable(joinType = JoinTypeEnum.LEFT_JOIN, on = "t1.school_id=t2.id")
public class StudentSchoolJoinVO {

	@Data
	public static class StudentVO extends StudentDO {
		// 特别注意：计算列的value和computed中的列都要加上表的别称，例如t1.
		@Column(value = "t1.nameWithHi", computed = "CONCAT(t1.name,'hi')")
		private String nameWithHi;
	}

	@RelatedColumn(localColumn = "t1.school_id", remoteColumn = "id")
	private SchoolDO schoolDO2;

	@JoinLeftTable
	private StudentVO studentDO;
	
	@JoinRightTable
	private SchoolDO schoolDO;

}
