package com.pugwoo.dbhelper.test.service.impl;

import com.pugwoo.dbhelper.DBHelper;
import com.pugwoo.dbhelper.annotation.RelatedColumn;
import com.pugwoo.dbhelper.test.entity.CourseDO;
import com.pugwoo.dbhelper.test.service.IGetCourseByStudentIdDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通过学生id查询该学生的所有课程，批量接口
 * @author pugwoo
 */
@Service
public class GetCourseByStudentIdServiceImpl implements IGetCourseByStudentIdDataService {

	@Autowired
	private DBHelper dbHelper;

	@Override
	public List<?> get(List<Object> values, RelatedColumn relatedColumn,
					   Class<?> localDOClass, Class<?> remoteDOClass) {

		// 说明：relatedColumn localDOClass remoteDOClass 这几个参数都是参考信息，可以不用

		// 这里只是演示，实际项目中可以SOA、或调用其它网络服务，或读取本地文件等方式读取数据
		List<CourseDO> list = dbHelper.getAll(CourseDO.class,
				"where student_id in (?)", values);
		return list;

	}

}
