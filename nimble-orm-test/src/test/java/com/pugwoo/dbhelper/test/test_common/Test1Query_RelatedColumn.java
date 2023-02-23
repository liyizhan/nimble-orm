package com.pugwoo.dbhelper.test.test_common;

import com.pugwoo.dbhelper.DBHelper;
import com.pugwoo.dbhelper.test.entity.CourseDO;
import com.pugwoo.dbhelper.test.entity.SchoolDO;
import com.pugwoo.dbhelper.test.entity.StudentDO;
import com.pugwoo.dbhelper.test.utils.CommonOps;
import com.pugwoo.dbhelper.test.vo.*;
import com.pugwoo.wooutils.collect.ListUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
public class Test1Query_RelatedColumn {

    @Autowired
    private DBHelper dbHelper;

    @Test
    public void testRelatedColumnWithLimit() {
        StudentDO studentDO = CommonOps.insertOne(dbHelper);

        CourseDO courseDO1 = new CourseDO();
        courseDO1.setName("math");
        courseDO1.setStudentId(studentDO.getId());
        courseDO1.setIsMain(true); // math是主课程
        dbHelper.insert(courseDO1);

        CourseDO courseDO2 = new CourseDO();
        courseDO2.setName("eng");
        courseDO2.setStudentId(studentDO.getId());
        courseDO2.setIsMain(true); // eng是主课程
        dbHelper.insert(courseDO2);

        CourseDO courseDO3 = new CourseDO();
        courseDO3.setName("chinese");
        courseDO3.setStudentId(studentDO.getId());
        courseDO3.setIsMain(true); // chinese是主课程
        dbHelper.insert(courseDO3);

        StudentDO studentDO2 = CommonOps.insertOne(dbHelper);

        List<StudentLimitVO> all = dbHelper.getAll(StudentLimitVO.class, "where id=? or id=?",
                studentDO.getId(), studentDO2.getId());
        for (StudentLimitVO a : all) {
            if (a.getId().equals(studentDO.getId())) {
                assert a.getMainCourses().size() == 2;
                assert a.getMainCourses().get(0).getIsMain();
                assert a.getMainCourses().get(1).getIsMain();
            }
            if (a.getId().equals(studentDO2.getId())) {
                assert a.getMainCourses().isEmpty();
            }
        }
    }

    @Test
    public void testRelatedColumnConditional() {
        // 构造数据：
        // 课程1 学生1 主课程
        // 课程1 学生2 非主课程
        // 课程2 学生1 非主课程
        // 课程2 学生2 主课程
        StudentDO student1 = CommonOps.insertOne(dbHelper);
        StudentDO student2 = CommonOps.insertOne(dbHelper);

        String course1 = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String course2 = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        CourseDO courseDO = new CourseDO();
        courseDO.setName(course1);
        courseDO.setStudentId(student1.getId());
        courseDO.setIsMain(true);
        dbHelper.insert(courseDO);
        Long id1 = courseDO.getId();

        courseDO = new CourseDO();
        courseDO.setName(course1);
        courseDO.setStudentId(student2.getId());
        courseDO.setIsMain(false);
        dbHelper.insert(courseDO);
        Long id2 = courseDO.getId();

        courseDO = new CourseDO();
        courseDO.setName(course2);
        courseDO.setStudentId(student1.getId());
        courseDO.setIsMain(false);
        dbHelper.insert(courseDO);
        Long id3 = courseDO.getId();

        courseDO = new CourseDO();
        courseDO.setName(course2);
        courseDO.setStudentId(student2.getId());
        courseDO.setIsMain(true);
        dbHelper.insert(courseDO);
        Long id4 = courseDO.getId();

        CourseVO courseVO = dbHelper.getOne(CourseVO.class, "where id=?", id1);
        assert courseVO.getMainCourseStudents().size() == 1;
        assert courseVO.getMainCourseStudents().get(0).getId().equals(student1.getId());
        assert courseVO.getMainCourseStudent().getId().equals(student1.getId());

        courseVO = dbHelper.getOne(CourseVO.class, "where id=?", id2);
        assert courseVO.getMainCourseStudents().isEmpty();
        assert courseVO.getMainCourseStudent() == null;

        courseVO = dbHelper.getOne(CourseVO.class, "where id=?", id3);
        assert courseVO.getMainCourseStudents().isEmpty();
        assert courseVO.getMainCourseStudent() == null;

        courseVO = dbHelper.getOne(CourseVO.class, "where id=?", id4);
        assert courseVO.getMainCourseStudents().size() == 1;
        assert courseVO.getMainCourseStudents().get(0).getId().equals(student2.getId());
        assert courseVO.getMainCourseStudent().getId().equals(student2.getId());

    }

    @Test
    public void testRelatedColumn() {

        SchoolDO schoolDO = new SchoolDO();
        schoolDO.setName("sysu");
        dbHelper.insert(schoolDO);

        StudentDO studentDO = CommonOps.insertOne(dbHelper);
        studentDO.setSchoolId(schoolDO.getId());
        dbHelper.update(studentDO);

        CourseDO courseDO1 = new CourseDO();
        courseDO1.setName("math");
        courseDO1.setStudentId(studentDO.getId());
        courseDO1.setIsMain(true); // math是主课程
        dbHelper.insert(courseDO1);

        CourseDO courseDO2 = new CourseDO();
        courseDO2.setName("eng");
        courseDO2.setStudentId(studentDO.getId());
        dbHelper.insert(courseDO2);

        StudentDO studentDO2  = CommonOps.insertOne(dbHelper);
        studentDO2.setSchoolId(schoolDO.getId());
        dbHelper.update(studentDO2);

        CourseDO courseDO3 = new CourseDO();
        courseDO3.setName("math");
        courseDO3.setStudentId(studentDO2.getId());
        courseDO3.setIsMain(true); // math是主课程
        dbHelper.insert(courseDO3);

        CourseDO courseDO4 = new CourseDO();
        courseDO4.setName("chinese");
        courseDO4.setStudentId(studentDO2.getId());
        dbHelper.insert(courseDO4);

        /////////////////// 下面是查询 ///////////////////

        StudentVO studentVO1 = dbHelper.getByKey(StudentVO.class, studentDO.getId());
        assert studentVO1 != null;
        assert studentVO1.getSchoolDO() != null;
        assert studentVO1.getSchoolDO().getId().equals(studentVO1.getSchoolId());
        assert studentVO1.getCourses() != null;
        assert studentVO1.getCourses().size() == 2;
        assert studentVO1.getCourses().get(0).getId().equals(courseDO1.getId())
                || studentVO1.getCourses().get(0).getId().equals(courseDO2.getId());
        assert studentVO1.getMainCourses().size() == 1 &&
                studentVO1.getMainCourses().get(0).getName().equals("math"); // math是主课程
        assert studentVO1.getNameWithHi().equals(studentVO1.getName() + "hi"); // 测试计算列

        // == handleRelatedColumn test
        StudentVOForHandleRelatedColumnOnly studentVO2 = new StudentVOForHandleRelatedColumnOnly();
        studentVO2.setId(studentDO.getId());
        studentVO2.setSchoolId(studentDO.getSchoolId());
        dbHelper.handleRelatedColumn(studentVO2);
        assert studentVO2 != null;
        assert studentVO2.getSchoolDO() != null;
        assert studentVO2.getSchoolDO().getId().equals(studentVO2.getSchoolId());
        assert studentVO2.getCourses() != null;
        assert studentVO2.getCourses().size() == 2;
        assert studentVO2.getCourses().get(0).getId().equals(courseDO1.getId())
                || studentVO2.getCourses().get(0).getId().equals(courseDO2.getId());
        assert studentVO2.getMainCourses().size() == 1 &&
                studentVO2.getMainCourses().get(0).getName().equals("math"); // math是主课程

        studentVO2 = new StudentVOForHandleRelatedColumnOnly();
        studentVO2.setId(studentDO.getId());
        studentVO2.setSchoolId(studentDO.getSchoolId());
        dbHelper.handleRelatedColumn(studentVO2, "courses", "schoolDO"); // 指定要的RelatedColumn
        assert studentVO2.getSchoolDO() != null;
        assert studentVO2.getSchoolDO().getId().equals(studentVO2.getSchoolId());
        assert studentVO2.getCourses() != null;
        assert studentVO2.getCourses().size() == 2;
        assert studentVO2.getCourses().get(0).getId().equals(courseDO1.getId())
                || studentVO2.getCourses().get(0).getId().equals(courseDO2.getId());
        assert studentVO2.getMainCourses() == null;

        // 转换成list处理，这个其实和上面这一段是一样的逻辑
        studentVO2 = new StudentVOForHandleRelatedColumnOnly();
        studentVO2.setId(studentDO.getId());
        studentVO2.setSchoolId(studentDO.getSchoolId());
        dbHelper.handleRelatedColumn(ListUtils.newList(studentVO2), "courses", "schoolDO");
        assert studentVO2.getSchoolDO() != null;
        assert studentVO2.getSchoolDO().getId().equals(studentVO2.getSchoolId());
        assert studentVO2.getCourses() != null;
        assert studentVO2.getCourses().size() == 2;
        assert studentVO2.getCourses().get(0).getId().equals(courseDO1.getId())
                || studentVO2.getCourses().get(0).getId().equals(courseDO2.getId());
        assert studentVO2.getMainCourses() == null;

        // END

        List<Long> ids = new ArrayList<Long>();
        ids.add(studentDO.getId());
        ids.add(studentDO2.getId());
        List<StudentVO> studentVOs = dbHelper.getAll(StudentVO.class,
                "where id in (?)", ids);
        assert studentVOs.size() == 2;
        for(StudentVO sVO : studentVOs) {
            assert sVO != null;
            assert sVO.getSchoolDO() != null;
            assert sVO.getSchoolDO().getId().equals(sVO.getSchoolId());
            assert sVO.getCourses() != null;
            assert sVO.getCourses().size() == 2;
            assert sVO.getMainCourses().size() == 1 &&
                    studentVO1.getMainCourses().get(0).getName().equals("math"); // math是主课程

            if(sVO.getId().equals(studentDO2.getId())) {
                assert
                        sVO.getCourses().get(0).getId().equals(courseDO3.getId())
                                || sVO.getCourses().get(1).getId().equals(courseDO4.getId());
            }

            assert sVO.getNameWithHi().equals(sVO.getName() + "hi"); // 测试计算列
        }

        // 测试innerClass
        SchoolWithInnerClassVO schoolVO = dbHelper.getByKey(SchoolWithInnerClassVO.class, schoolDO.getId());
        assert schoolVO != null && schoolVO.getId().equals(schoolDO.getId());
        assert schoolVO.getStudents().size() == 2;
        for(SchoolWithInnerClassVO.StudentVO s : schoolVO.getStudents()) {
            assert s != null && s.getId() != null && s.getCourses().size() == 2;
        }
    }

}
