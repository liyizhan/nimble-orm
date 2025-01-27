package com.pugwoo.dbhelper.test.test_common;

import com.pugwoo.dbhelper.DBHelper;
import com.pugwoo.dbhelper.json.NimbleOrmJSON;
import com.pugwoo.dbhelper.test.entity.CourseDO;
import com.pugwoo.dbhelper.test.entity.SchoolDO;
import com.pugwoo.dbhelper.test.entity.StudentDO;
import com.pugwoo.dbhelper.test.utils.CommonOps;
import com.pugwoo.wooutils.collect.ListUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
public class Test4Delete_SoftDeleteTable {

    @Autowired
    private DBHelper dbHelper;

    @Test
    public void testDelete() {
        List<StudentDO> studentDOS = CommonOps.insertBatch(dbHelper, 1);

        // 设置完整的字段
        StudentDO studentDO = studentDOS.get(0);
        studentDO.setAge(new Random().nextInt(100));
        studentDO.setIntro("i like basketball".getBytes());
        long schoolId = new Random().nextLong();
        studentDO.setSchoolId(schoolId % 100000L);
        SchoolDO schoolDO = new SchoolDO();
        schoolDO.setId(studentDO.getSchoolId());
        schoolDO.setName(UUID.randomUUID().toString());
        studentDO.setSchoolSnapshot(schoolDO);

        List<CourseDO> courses = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            CourseDO courseDO = new CourseDO();
            courseDO.setId(new Random().nextLong() % 100000L);
            courseDO.setName(UUID.randomUUID().toString());
            courses.add(courseDO);
        }
        studentDO.setCourseSnapshot(courses);

        assert dbHelper.update(studentDO) == 1;

        assert dbHelper.delete(studentDOS.get(0)) == 1;

        // 测试数据已经被删除了
        assert dbHelper.getOne(StudentDO.class, "where id=?", studentDO.getId()) == null;

        // 从另外一张表里查出数据，再比较
        Map<Class<?>, String> tableNames = new HashMap<>();
        tableNames.put(StudentDO.class, "t_student_del");

        DBHelper.withTableNames(tableNames, () -> {
            StudentDO s = dbHelper.getOne(StudentDO.class, "where id=?", studentDO.getId());
            assert s != null;
            assert s.getId().equals(studentDO.getId());
            assert s.getCreateTime().equals(studentDO.getCreateTime());
            assert s.getName().equals(studentDO.getName());
            assert s.getAge().equals(studentDO.getAge());
            assert new String(s.getIntro()).equals(new String(studentDO.getIntro()));
            assert s.getSchoolId().equals(studentDO.getSchoolId());
            assert s.getSchoolSnapshot().getId().equals(studentDO.getSchoolSnapshot().getId());
            assert s.getSchoolSnapshot().getName().equals(studentDO.getSchoolSnapshot().getName());

            assert NimbleOrmJSON.toJson(s.getCourseSnapshot()).equals(NimbleOrmJSON.toJson(studentDO.getCourseSnapshot()));
        });
    }

    @Test
    public void testDeleteList() {
        // 测试删除list的情况，这里就只检查name就好了，没有全字段检查了
        List<StudentDO> studentDOS = CommonOps.insertBatch(dbHelper, 9);

        List<Long> ids = ListUtils.transform(studentDOS, o -> o.getId());
        Map<Long, StudentDO> map = ListUtils.toMap(studentDOS, o -> o.getId(), o -> o);

        assert dbHelper.delete(studentDOS) == 9;

        // 测试数据已经被删除了
        assert dbHelper.getAll(StudentDO.class, " where id in (?)", ids).size() == 0;

        // 从另外一张表里查出数据，再比较
        Map<Class<?>, String> tableNames = new HashMap<>();
        tableNames.put(StudentDO.class, "t_student_del");

        DBHelper.withTableNames(tableNames, () -> {
            dbHelper.getAll(StudentDO.class, " where id in (?)", ids).forEach(s -> {
                assert ids.contains(s.getId());
                StudentDO oldStudent = map.get(s.getId());
                assert s.getName().equals(oldStudent.getName());
            });
        });
    }

    @Test
    public void testDeletePostSql() {
        String prefix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        List<StudentDO> studentDOS = CommonOps.insertBatch(dbHelper, 9, prefix);

        Map<Long, StudentDO> map = ListUtils.toMap(studentDOS, o -> o.getId(), o -> o);

        assert dbHelper.delete(StudentDO.class, "where name like ?", prefix + "%") == 9;

        // 测试数据已经被删除了
        assert dbHelper.getAll(StudentDO.class, " where name like ?", prefix + "%").size() == 0;

        // 从另外一张表里查出数据，再比较
        Map<Class<?>, String> tableNames = new HashMap<>();
        tableNames.put(StudentDO.class, "t_student_del");

        DBHelper.withTableNames(tableNames, () -> {
            dbHelper.getAll(StudentDO.class, " where name like ?", prefix + "%").forEach(s -> {
                assert map.containsKey(s.getId());
                StudentDO oldStudent = map.get(s.getId());
                assert s.getName().equals(oldStudent.getName());
            });
        });
    }
}
