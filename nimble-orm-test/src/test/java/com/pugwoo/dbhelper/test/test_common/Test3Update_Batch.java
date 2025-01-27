package com.pugwoo.dbhelper.test.test_common;

import com.pugwoo.dbhelper.DBHelper;
import com.pugwoo.dbhelper.annotation.Column;
import com.pugwoo.dbhelper.annotation.Table;
import com.pugwoo.dbhelper.exception.CasVersionNotMatchException;
import com.pugwoo.dbhelper.exception.NullKeyValueException;
import com.pugwoo.dbhelper.test.entity.*;
import com.pugwoo.dbhelper.test.utils.CommonOps;
import com.pugwoo.wooutils.collect.ListUtils;
import com.pugwoo.wooutils.collect.MapUtils;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

/**
 * 测试批量更新
 */
@SpringBootTest
public class Test3Update_Batch {

    @Autowired
    private DBHelper dbHelper;

    @Test
    public void testUpdateBatch() {
        // 插入11条数据，然后再批量update
        List<StudentDO> list = CommonOps.insertBatch(dbHelper, 11);
        List<Long> ids = ListUtils.transform(list, IdableSoftDeleteBaseDO::getId);
        Map<Long, StudentDO> map = ListUtils.toMap(list, IdableSoftDeleteBaseDO::getId, o -> o);

        for (StudentDO studentDO : list) {
            studentDO.setName(studentDO.getName() + "x");
        }

        int rows = dbHelper.update(list);
        assert rows == list.size();

        // update后，再查询一次，看看数据是否正确
        List<StudentDO> all = dbHelper.getAll(StudentDO.class, "where id in (?)", ids);
        assert all.size() == list.size();
        for (StudentDO studentDO : all) {
            assert studentDO.getName().equals(map.get(studentDO.getId()).getName());
        }

        // 软删除2个，再批量update
        dbHelper.delete(list.get(3));
        dbHelper.delete(list.get(8));

        for (StudentDO studentDO : list) {
            studentDO.setName(studentDO.getName() + "y");
        }

        rows = dbHelper.update(list);
        assert rows == list.size() - 2; // 软删除了2个

        // update后，再查询一次，看看数据是否正确
        all = dbHelper.getAll(StudentDO.class, "where id in (?)", ids);
        assert all.size() == list.size() - 2;
        for (StudentDO studentDO : all) {
            assert studentDO.getName().equals(map.get(studentDO.getId()).getName());
        }
    }

    // 批量update有null值的情况，看看数据库的值会不会被改，期望是不改，保留原值
    @Test
    public void testBatchUpdateWithNullValue() {
        List<StudentDO> list = CommonOps.insertBatch(dbHelper, 11);
        ListUtils.forEach(list, o -> {
            o.setSchoolId(o.getId());
            o.setName(o.getName() + "x"); // 验证至少修改2个字段
        });
        assert dbHelper.update(list) == 11;

        Map<Long, StudentDO> map = ListUtils.toMap(list, IdableSoftDeleteBaseDO::getId, o -> o);
        List<Long> ids = ListUtils.transform(list, IdableSoftDeleteBaseDO::getId);

        // 验证是否修改成功
        List<StudentDO> students = dbHelper.getAll(StudentDO.class, "where id in (?)", ids);
        for (StudentDO student : students) {
            assert Objects.equals(student.getSchoolId(), student.getId());
            assert student.getName().equals(map.get(student.getId()).getName());
        }

        // 将前9条设置为null，再更新
        for (int i = 0; i < 9; i++) {
            students.get(i).setSchoolId(null);
        }
        for (int i = 0; i < 11; i++) {
            students.get(i).setName(students.get(i).getName() + "y");
        }

        map = ListUtils.toMap(students, IdableSoftDeleteBaseDO::getId, o -> o);

        assert dbHelper.update(students) == 11; // 因为有update time的值，所以有更新

        // 重新查回来，验证null值实际上没有被修改
        students = dbHelper.getAll(StudentDO.class, "where id in (?)", ids);
        for (StudentDO student : students) {
            assert Objects.equals(student.getSchoolId(), student.getId()); // null值不会被修改
            assert student.getName().endsWith("xy"); // 名字则被修改了
            assert student.getName().equals(map.get(student.getId()).getName());
        }
    }

    // 批量update有casVersion的情况
    @Test
    public void testCasVersionUpdate() {
        String name = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        List<CasVersionDO> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            CasVersionDO cas = new CasVersionDO();
            cas.setName(name);
            list.add(cas);
        }

        int rows = dbHelper.insertBatchWithoutReturnId(list);
        assert rows == 7;
        for (CasVersionDO cas : list) {
            assert cas.getVersion() == 1;
        }

        // 查询回来
        list = dbHelper.getAll(CasVersionDO.class, "where name=?", name);
        assert list.size() == 7;
        for (CasVersionDO cas : list) {
            assert cas.getVersion() == 1;
        }

        // 再修改一次
        for (CasVersionDO cas : list) {
            cas.setName(cas.getName() + "x");
        }
        assert dbHelper.update(list) == 7;
        for (CasVersionDO cas : list) {
            assert cas.getVersion() == 2;
        }

        // 再查询回来
        list = dbHelper.getAll(CasVersionDO.class, "where name=?", name + "x");
        assert list.size() == 7;
        for (CasVersionDO cas : list) {
            assert cas.getVersion() == 2;
            assert (name + "x").equals(cas.getName());
        }

        // 故意改错2条casversion
        list.get(1).setVersion(1);
        list.get(4).setVersion(4);
        for (CasVersionDO cas : list) {
            cas.setName(cas.getName() + "y");
        }

        boolean isThrow = false;
        try {
            dbHelper.update(list);
        } catch (CasVersionNotMatchException e) {
            isThrow = true;
            assert e.getAffectedRows() == 7 - 2; // 2条casversion不匹配，所以只有5条被修改
            // 此时业务方可以选择回滚，这里并不回滚
        }
        assert isThrow;

        // 查询回来验证
        list = dbHelper.getAll(CasVersionDO.class, "where name=?", name + "x");
        assert list.size() == 2;

        list = dbHelper.getAll(CasVersionDO.class, "where name=?", name + "xy");
        assert list.size() == 5;

        // 全部再查回来一次
        list = dbHelper.getAll(CasVersionDO.class, "where name like ?", name + "%");
        assert list.size() == 7;

        // 故意改掉所有的casversion
        for (CasVersionDO cas : list) {
            cas.setVersion(cas.getVersion() + 1);
            cas.setName(cas.getName() + "z");
        }

        isThrow = false;
        try {
            dbHelper.update(list);
        } catch (CasVersionNotMatchException e) {
            isThrow = true;
            assert e.getAffectedRows() == 0;
        }
        assert isThrow;

        // 再查询回来验证
        list = dbHelper.getAll(CasVersionDO.class, "where name like ?", name + "%");
        assert list.size() == 7;
        for (CasVersionDO cas : list) {
            assert !cas.getName().endsWith("z"); // 期望没有修改
        }

        // 测试update时，有null值的情况
        list = dbHelper.getAll(CasVersionDO.class, "where name like ?", name + "%");
        assert list.size() == 7;
        for (CasVersionDO cas : list) {
            cas.setName(cas.getName() + "z");
        }
        list.get(0).setName(null);
        list.get(5).setName(null);
        list.get(6).setName(null); // 3个设置name为null，即不修改
        assert dbHelper.update(list) == 7;

        // 再查询回来验证
        list = dbHelper.getAll(CasVersionDO.class, "where name like ?", name + "%");
        assert list.size() == 7;
        assert ListUtils.filter(list, o -> o.getName().endsWith("z")).size() == 4; // 4个被修改了
        assert ListUtils.filter(list, o -> !o.getName().endsWith("z")).size() == 3; // 3个没有被修改

    }

    @Test
    public void testUpdateJSON() {
        // 正常的json do
        {
            List<JsonDO> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                JsonDO jsonDO = new JsonDO();
                jsonDO.setJson2(MapUtils.of("one", UUID.randomUUID().toString(),
                        "two", UUID.randomUUID().toString()));
                list.add(jsonDO);
            }
            assert dbHelper.insert(list) == 4;

            // 更新
            for (JsonDO jsonDO : list) {
                jsonDO.setJson2(MapUtils.of("one", UUID.randomUUID().toString(),
                        "two", UUID.randomUUID().toString(),
                        "three", UUID.randomUUID().toString()));
            }
            assert dbHelper.update(list) == 4;

            // 查询回来验证
            for (JsonDO jsonDO : list) {
                JsonDO one = dbHelper.getOne(JsonDO.class, "where id=?", jsonDO.getId());
                assert one.getJson2().size() == 3;
                assert one.getJson2().get("one").equals(jsonDO.getJson2().get("one"));
                assert one.getJson2().get("two").equals(jsonDO.getJson2().get("two"));
                assert one.getJson2().get("three").equals(jsonDO.getJson2().get("three"));
            }
        }

        // 测试带cas+json的DO
        {
            List<CasVersionWithJsonDO> list2 = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                CasVersionWithJsonDO jsonDO = new CasVersionWithJsonDO();
                jsonDO.setName(MapUtils.of("a",
                        UUID.randomUUID().toString().replace("-","").substring(0, 16)));
                list2.add(jsonDO);
            }
            assert dbHelper.insert(list2) == 3;

            // 检查cas版本
            for (CasVersionWithJsonDO jsonDO : list2) {
                assert jsonDO.getVersion() == 1;
            }

            // 更新
            for (CasVersionWithJsonDO jsonDO : list2) {
                jsonDO.setName(MapUtils.of("a",
                        UUID.randomUUID().toString().replace("-","").substring(0, 16)));
            }
            assert dbHelper.update(list2) == 3;

            // 查询回来验证
            for (CasVersionWithJsonDO jsonDO : list2) {
                CasVersionWithJsonDO one = dbHelper.getOne(CasVersionWithJsonDO.class, "where id=?", jsonDO.getId());
                assert one.getName().size() == 1;
                assert one.getName().get("a").equals(jsonDO.getName().get("a"));
                assert one.getVersion().equals(2);
            }
        }
    }

    @Test
    public void testUpdateDifferentDO() {
        List<Object> list = new ArrayList<>();

        StudentDO studentDO = new StudentDO();
        studentDO.setName("test");
        dbHelper.insert(studentDO);
        list.add(studentDO);

        SchoolDO schoolDO = new SchoolDO();
        schoolDO.setName("sysu");
        dbHelper.insert(schoolDO);
        list.add(schoolDO);

        // 修改name
        studentDO.setName(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        schoolDO.setName(UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        assert dbHelper.update(list) == 2;

        // 查回来检查数据
        StudentDO one = dbHelper.getOne(StudentDO.class, "where id=?", studentDO.getId());
        assert one.getName().equals(studentDO.getName());

        SchoolDO b = dbHelper.getOne(SchoolDO.class, "where id=?", schoolDO.getId());
        assert b.getName().equals(schoolDO.getName());

    }

    @Test
    public void testSomeExCase() {
        List<StudentDO> studentDOS = CommonOps.insertBatch(dbHelper, 3);

        // 测试没有非主键的DO
        List<StudentOnlyKeyDO> list = new ArrayList<>();
        for (int i = 0; i < studentDOS.size(); i++) {
            StudentOnlyKeyDO student = new StudentOnlyKeyDO();
            student.setId(studentDOS.get(i).getId());
            list.add(student);
        }
        assert dbHelper.update(list) == 0; // 只有主键，所以不会有任何更新，即使id存在


        // 测试多个casVersion注解的情况，此时会抛异常
        List<MultiCasVersionDO> list2 = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MultiCasVersionDO d = new MultiCasVersionDO();
            d.setId(i + 1L);
            list2.add(d);
        }
        boolean isThrow = false;
        try {
            dbHelper.update(list2);
        } catch (Exception e) {
            isThrow = true;
            assert e.getMessage().contains("has more than one casVersion column");
        }
        assert isThrow;

        // 测试更新student，但是传入的非key值都是null，等于不需要更新
        List<StudentKeyAndNameDO> list3 = new ArrayList<>();
        for (StudentDO studentDO : studentDOS) {
            StudentKeyAndNameDO d = new StudentKeyAndNameDO();
            d.setId(studentDO.getId());
            list3.add(d);
        }
        assert dbHelper.update(list3) == 0; // 所有非主键列都是null

        // 测试update提供了null的主键
        studentDOS.get(0).setId(null);
        isThrow = false;
        try {
            dbHelper.update(studentDOS);
        } catch (NullKeyValueException e) {
            isThrow = true;
        }
        assert isThrow;
    }

    @Data
    @Table("t_student")
    public static class StudentOnlyKeyDO {
        @Column(value = "id", isKey = true, isAutoIncrement = true)
        private Long id;
    }

    @Data
    @Table("t_student")
    public static class StudentKeyAndNameDO {
        @Column(value = "id", isKey = true, isAutoIncrement = true)
        private Long id;

        @Column(value = "name")
        private String name;
    }

    @Data
    @Table("t_student")
    public static class MultiCasVersionDO {

        @Column(value = "id", isKey = true, isAutoIncrement = true)
        private Long id;

        @Column(value = "age", casVersion = true)
        private Integer age;

        @Column(value = "school_id", casVersion = true)
        private Long schoolId;

    }

    @Data
    @Table("t_cas_version")
    public static class CasVersionWithJsonDO {

        @Column(value = "id", isKey = true, isAutoIncrement = true)
        private Integer id;

        @Column(value = "name", isJSON = true)
        private Map<String, Object> name;

        @Column(value = "version", casVersion = true)
        private Integer version;

    }

    @Test
    public void testUpdateBatchBenchmark() {
        String name = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        List<StudentDO> students = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            StudentDO student = new StudentDO();
            student.setName(name);
            students.add(student);
        }

        dbHelper.insertBatchWithoutReturnId(students);

        List<StudentDO> all = dbHelper.getAll(StudentDO.class, "where name=?", name);

        all.forEach(o -> o.setName(o.getName() + "x"));

        long start = System.currentTimeMillis();
        assert dbHelper.update(all) == 1000;

        // 这种方式需要60秒，而上面的方式只需要1秒
//        for (int i = 0; i < 1000; i++) {
//            dbHelper.update(all.get(i));
//        }

        long end = System.currentTimeMillis();
        assert end - start < 3000; // 一般600ms完成，最多3秒

        System.out.println("cost: " + (end - start) + "ms");
    }
}
