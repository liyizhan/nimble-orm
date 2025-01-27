package com.pugwoo.dbhelper.test.test_common;

import com.pugwoo.dbhelper.DBHelper;
import com.pugwoo.dbhelper.enums.FeatureEnum;
import com.pugwoo.dbhelper.exception.NotAllowQueryException;
import com.pugwoo.dbhelper.exception.NullKeyValueException;
import com.pugwoo.dbhelper.model.PageData;
import com.pugwoo.dbhelper.test.entity.*;
import com.pugwoo.dbhelper.test.utils.CommonOps;
import com.pugwoo.dbhelper.test.vo.*;
import com.pugwoo.wooutils.collect.ListUtils;
import com.pugwoo.wooutils.collect.MapUtils;
import com.pugwoo.wooutils.lang.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 测试读操作相关
 */
@SpringBootTest
public class TestDBHelper_query {

    @Autowired
    private DBHelper dbHelper;

    @Test 
    public void testSameTableNameAs() {
        StudentDO studentDO = CommonOps.insertOne(dbHelper);
        StudentCalVO db = dbHelper.getOne(StudentCalVO.class, "where id=?", studentDO.getId());
        assert db != null;
        assert db.getNameWithHi() != null && db.getNameWithHi().endsWith("hi");
    }

    @Test 
    public void testGetByKey() {
        Long id = CommonOps.insertOne(dbHelper).getId();

        StudentDO student2 = dbHelper.getByKey(StudentDO.class, id);
        assert student2 != null && student2.getId().equals(id);

        // student的时分秒不能全为0
        Date createTime = student2.getCreateTime();
        assert createTime != null;
        assert !(DateUtils.getHour(createTime) == 0 &&
                 DateUtils.getMinute(createTime) == 0 &&
                 DateUtils.getSecond(createTime) == 0);

        // student的时间戳在当前时间10秒以内才算合格
        assert System.currentTimeMillis() - createTime.getTime() < 10000;

        // 测试一个异常情况
        boolean isThrowException = false;
        try {
            dbHelper.getByKey(StudentDO.class, null);
        } catch (Exception e) {
            if (e instanceof NullKeyValueException) {
                isThrowException = true;
            }
        }
        assert isThrowException;

        // getByKey不支持virtual table
        isThrowException = false;
        try {
            dbHelper.getByKey(StudentVirtualTableVO.class, 1);
        } catch (NotAllowQueryException e) {
            isThrowException = true;
        }
        assert isThrowException;
    }



    @Test 
    public void testExists() {
        StudentDO studentDO = CommonOps.insertOne(dbHelper);
        assert dbHelper.isExist(StudentDO.class, null);
        assert dbHelper.isExist(StudentDO.class, "where id=?", studentDO.getId());
        assert dbHelper.isExistAtLeast(1, StudentDO.class,
                "where id=?", studentDO.getId());

        assert !dbHelper.isExistAtLeast(2, StudentDO.class,
                "where id=?", studentDO.getId());
    }

    @Test 
    public void testGetList() {
        // 测试获取全部
        List<StudentDO> list = dbHelper.getAll(StudentDO.class);
        System.out.println("total:" + list.size());
        for(StudentDO studentDO : list) {
            System.out.println(studentDO);
        }

        System.out.println("===============================");

        // 测试获取有条件的查询
        Long[] ids = new Long[3];
        ids[0] = 2L;
        ids[1] = 4L;
        ids[2] = 6L;
        //List<StudentDO> list2 = dbHelper.getAll(StudentDO.class, "where id in (?)",
        //		ids); // 这样是错误的范例，getAll只会取ids的第一个参数传入in (?)中
        List<StudentDO> list2 = dbHelper.getAll(StudentDO.class, "where id in (?)",
                ids, 1); // 这是一种hack的写法，后面带上的参数1，可以让Java把ids当作单个参数处理
        System.out.println("total:" + list2.size());
        for(StudentDO studentDO : list2) {
            System.out.println(studentDO);
        }

        System.out.println("===============================");
    }

    @Test 
    public void testGetByExample() {
        StudentDO studentDO = CommonOps.insertOne(dbHelper);

        StudentDO example = new StudentDO();
        example.setName(studentDO.getName());

        List<StudentDO> byExample = dbHelper.getByExample(example, 10);
        assert byExample.size() == 1;
        assert byExample.get(0).getId().equals(studentDO.getId());
        assert byExample.get(0).getName().equals(studentDO.getName());

        example.setIntro(studentDO.getIntro());
        byExample = dbHelper.getByExample(example, 10);
        assert byExample.size() == 1;
        assert byExample.get(0).getId().equals(studentDO.getId());
        assert byExample.get(0).getName().equals(studentDO.getName());
    }

    @Test 
    public void testGetByArray() {
        // 但是这种写法容易有歧义，推荐传入List参数值
        List<StudentDO> list = dbHelper.getAll(StudentDO.class, "where id in (?)", new long[]{50,51,52});
        System.out.println(list.size());
        list = dbHelper.getAll(StudentDO.class, "where id in (?)", new int[]{50,51,52});
        System.out.println(list.size());
        list = dbHelper.getAll(StudentDO.class, "where id in (?)", new short[]{50,51,52});
        System.out.println(list.size());
        list = dbHelper.getAll(StudentDO.class, "where id in (?)", new char[]{50,51,52});
        System.out.println(list.size());
        list = dbHelper.getAll(StudentDO.class, "where id in (?)", new float[]{50,51,52});
        System.out.println(list.size());
        list = dbHelper.getAll(StudentDO.class, "where id in (?)", new double[]{50,51,52});
        System.out.println(list.size());

        // 测试空list或空set
        list = dbHelper.getAll(StudentDO.class, "where id in (?)", new ArrayList<Long>());
        assert list.isEmpty();
        list = dbHelper.getAll(StudentDO.class, "where id in (?)", new HashSet<Long>());
        assert list.isEmpty();
    }

    @Test
    public void testGetJoin() {
        SchoolDO schoolDO = new SchoolDO();
        schoolDO.setName("sysu");
        dbHelper.insert(schoolDO);

        StudentDO studentDO = CommonOps.insertOne(dbHelper);
        studentDO.setSchoolId(schoolDO.getId());
        dbHelper.update(studentDO);

        StudentDO studentDO2 = CommonOps.insertOne(dbHelper);
        studentDO2.setSchoolId(schoolDO.getId());
        dbHelper.update(studentDO2);

        PageData<StudentSchoolJoinVO> pageData = dbHelper.getPage(StudentSchoolJoinVO.class, 1, 10);
        assert pageData.getData().size() > 0;
        for(StudentSchoolJoinVO vo : pageData.getData()) {
            assert vo.getStudentDO() != null;
        }

        List<StudentSchoolJoinVO> all = dbHelper.getAll(StudentSchoolJoinVO.class, "where t1.id in (?)",
                ListUtils.newList(studentDO.getId(), studentDO2.getId()));
        assert all.size() == 2;
        assert all.get(0).getSchoolDO2().getId().equals(schoolDO.getId());
        assert all.get(1).getSchoolDO2().getId().equals(schoolDO.getId());
        assert all.get(0).getSchoolDO().getId().equals(schoolDO.getId());
        assert all.get(1).getSchoolDO().getId().equals(schoolDO.getId());
        assert all.get(0).getSchoolDO3().getId().equals(schoolDO.getId());
        assert all.get(1).getSchoolDO3().getId().equals(schoolDO.getId());
        assert all.get(0).getVo2().getSchoolDO().getId().equals(schoolDO.getId());
        assert all.get(1).getVo2().getSchoolDO().getId().equals(schoolDO.getId());
        assert all.get(0).getVo3().getSchoolDO().getId().equals(schoolDO.getId());
        assert all.get(1).getVo3().getSchoolDO().getId().equals(schoolDO.getId());

        pageData = dbHelper.getPage(StudentSchoolJoinVO.class, 1, 10,
                "where t1.name like ?", "nick%");
        assert pageData.getData().size() > 0;
        for(StudentSchoolJoinVO vo : pageData.getData()) {
            assert vo.getStudentDO() != null;
        }

        long total = dbHelper.getCount(StudentSchoolJoinVO.class);
        assert total > 0;
        total = dbHelper.getCount(StudentSchoolJoinVO.class, "where t1.name like ?", "nick%");
        assert total > 0;

        // right join test
        PageData<StudentSchoolJoinVO2> pageData2 = dbHelper.getPage(StudentSchoolJoinVO2.class, 1, 10);
        assert pageData2.getData().size() > 0;
        for(StudentSchoolJoinVO2 vo : pageData2.getData()) {
            assert vo.getStudentDO() != null;
        }

    }

    @Test 
    public void testDateTime() {

        dbHelper.turnOnFeature(FeatureEnum.LOG_SQL_AT_INFO_LEVEL);

        StudentDO studentDO = CommonOps.insertOne(dbHelper);
        dbHelper.delete(studentDO);

        StudentWithLocalDateTimeDO one = dbHelper.getOne(StudentWithLocalDateTimeDO.class,
                "where id=?", studentDO.getId());

        assert one.getCreateTime() != null;
        assert one.getUpdateTime() != null;
        assert one.getDeleteTime() != null;
    }

    /**测试软删除DO查询条件中涉及到OR条件的情况*/
    @Test 
    public void testQueryWithDeletedAndOr() {
        // 先清表
        dbHelper.delete(StudentDO.class, "where 1=1");

        CommonOps.insertBatch(dbHelper, 10);
        dbHelper.delete(StudentDO.class, "where 1=1"); // 确保至少有10条删除记录

        CommonOps.insertBatch(dbHelper, 10);
        List<StudentDO> all = dbHelper.getAll(StudentDO.class, "where 1=1 or 1=1"); // 重点
        assert all.size() == 10; // 只应该查出10条记录，而不是20条以上的记录
        for(StudentDO studentDO : all) {
            assert !studentDO.getDeleted();
        }

        all = dbHelper.getAll(StudentDO.class, "where 1=1 and 1=1 or 1=1 or 1=1");
        assert all.size() == 10;
    }

    /**测试join真删除的类*/
    @Test 
    public void testJoinTrueDelete() {
        StudentDO studentDO = CommonOps.insertOne(dbHelper);
        StudentSelfTrueDeleteJoinVO joinVO = dbHelper.getOne(StudentSelfTrueDeleteJoinVO.class, "where t1.id=?", studentDO.getId());
        assert joinVO.getStudent1().getId().equals(studentDO.getId());
        assert joinVO.getStudent2().getId().equals(studentDO.getId());
    }

    @Test
    public void testGetRawIfColumnNotExist() {
        final StudentDO studentDO1 = CommonOps.insertOne(dbHelper);

        // 这里故意只查回id，而DO类是要收id和name的，默认情况下不会报错
        List<StudentForRawDO> list = dbHelper.getRaw(StudentForRawDO.class,
                "select id from t_student where name=?", studentDO1.getName());

        assert list.get(0).getId().equals(studentDO1.getId());
        assert list.get(0).getName() == null;

        dbHelper.turnOnFeature(FeatureEnum.THROW_EXCEPTION_IF_COLUMN_NOT_EXIST);
        boolean isThrowEx = false;
        try {
            list = dbHelper.getRaw(StudentForRawDO.class,
                    "select id from t_student where name=?", studentDO1.getName());
        } catch (Exception e) {
            isThrowEx = true;
        }
        assert isThrowEx;

        dbHelper.turnOffFeature(FeatureEnum.THROW_EXCEPTION_IF_COLUMN_NOT_EXIST);
    }

    @Test 
    public void testGetRaw() {
        final StudentDO studentDO1 = CommonOps.insertOne(dbHelper);
        final StudentDO studentDO2 = CommonOps.insertOne(dbHelper);

        List<StudentForRawDO> list = dbHelper.getRaw(StudentForRawDO.class,
                "select id,name from t_student where name=?", studentDO1.getName());

        assert list.size() == 1;
        assert list.get(0).getName().equals(studentDO1.getName());

        List<Map> list2 = dbHelper.getRaw(Map.class,
                "select id,name from t_student where name=?", studentDO1.getName());
        assert list2.size() == 1;
        assert list2.get(0).get("name").equals(studentDO1.getName());


        Map<String, Object> params = new HashMap<>();
        params.put("name", studentDO1.getName());
        list = dbHelper.getRaw(StudentForRawDO.class,
                "select id,name from t_student where name=:name",
                params);

        assert list.size() == 1;
        assert list.get(0).getName().equals(studentDO1.getName());

        long count = dbHelper.getRawOne(Long.class, "select count(*) from t_student where name=:name",
                params);
        assert count == 1;

        List<String> names = new ArrayList<String>();
        names.add(studentDO1.getName());
        names.add(studentDO2.getName());
        count = dbHelper.getRawOne(Long.class, "select count(*) from t_student where name in (?)",
                names);
        assert count == 2;

        List<Long> sum = dbHelper.getRaw(Long.class, "select sum(age) from t_student where name=?",
                UUID.randomUUID().toString());
        assert sum.get(0) == 0;

        // test get rawOne
        Long sum2 = dbHelper.getRawOne(Long.class, "select sum(age) from t_student where name=?",
                UUID.randomUUID().toString());
        assert sum2 == 0;

        sum2 = dbHelper.getRawOne(Long.class, "select sum(age) from t_student where name=:name",
                MapUtils.of("name", UUID.randomUUID().toString()));
        assert sum2 == 0;

        // 测试没有参数的
        assert dbHelper.getRawOne(Long.class, "select count(*) from t_student") > 0;
        assert dbHelper.getRawOne(Long.class, "select count(*) from t_student", new HashMap<>()) > 0;

        // 测试查询不到值的
        assert dbHelper.getRawOne(StudentDO.class, "select * from t_student where name=?",
                UUID.randomUUID().toString()) == null;
        assert dbHelper.getRawOne(StudentDO.class, "select * from t_student where name=:name",
                MapUtils.of("name", UUID.randomUUID().toString())) == null;
    }

    @Test 
    public void testGetRawWithBasicType() {

        StudentDO studentDO1 = CommonOps.insertOne(dbHelper);
        StudentDO studentDO2 = CommonOps.insertOne(dbHelper);
        StudentDO studentDO3 = CommonOps.insertOne(dbHelper);

        List<String> studentNames = dbHelper.getRaw(String.class, "select name from t_student where deleted=0");

        assert studentNames.contains(studentDO1.getName());
        assert studentNames.contains(studentDO2.getName());
        assert studentNames.contains(studentDO3.getName());

        List<Integer> count = dbHelper.getRaw(Integer.class, "select count(*) from t_student where deleted=0");
        assert count.get(0) >= 3;

        List<Boolean> bools = dbHelper.getRaw(Boolean.class, "select 1");
        assert bools.get(0);
        bools = dbHelper.getRaw(Boolean.class, "select 0");
        assert !bools.get(0);

        List<Byte> bytes = dbHelper.getRaw(Byte.class, "select 'a'");
        assert bytes.get(0) == 97;

        List<byte[]> bytes2 = dbHelper.getRaw(byte[].class, "select 'a'");
        assert bytes2.get(0)[0] == 97;

        List<Short> count2 = dbHelper.getRaw(Short.class, "select count(*) from t_student where deleted=0");
        assert count2.get(0) >= 3;

        List<Float> count3 = dbHelper.getRaw(Float.class, "select count(*) from t_student where deleted=0");
        assert count3.get(0) >= 3;

        List<Double> count4 = dbHelper.getRaw(Double.class, "select count(*) from t_student where deleted=0");
        assert count4.get(0) >= 3;

        List<BigDecimal> count5 = dbHelper.getRaw(BigDecimal.class, "select count(*) from t_student where deleted=0");
        assert count5.get(0).compareTo(BigDecimal.valueOf(3)) >= 0;

        List<Date> dates = dbHelper.getRaw(Date.class, "select now()");
        assert dates.get(0) != null;

        List<LocalDateTime> dates2 = dbHelper.getRaw(LocalDateTime.class, "select now()");
        assert dates2.get(0) != null;

        List<LocalDate> dates3 = dbHelper.getRaw(LocalDate.class, "select now()");
        assert dates3.get(0) != null;

        List<LocalTime> dates4 = dbHelper.getRaw(LocalTime.class, "select now()");
        assert dates4.get(0) != null;

        List<java.sql.Date> dates5 = dbHelper.getRaw(java.sql.Date.class, "select now()");
        assert dates5.get(0) != null;
        List<java.sql.Time> dates6 = dbHelper.getRaw(java.sql.Time.class, "select now()");
        assert dates6.get(0) != null;
        List<java.sql.Timestamp> dates7 = dbHelper.getRaw(java.sql.Timestamp.class, "select now()");
        assert dates7.get(0) != null;

    }
    
    @Test
    public void testSum() {
        // 故意让sum的记录不存在
        StudentSumVO one = dbHelper.getOne(StudentSumVO.class, "where id = -1");
        assert one.getAgeSum() == 0;

        dbHelper.delete(StudentDO.class, "where 1=1");
        CommonOps.insertBatch(dbHelper, 30);

        PageData<StudentSumVO> pageData = dbHelper.getPage(StudentSumVO.class,
                1, 10, "group by name order by ageSum");

        assert pageData.getTotal() == 30;
        assert pageData.getData().size() == 10;

    }

    /**
     * 测试相同列字段名称的情况
     */
    
    @Test
    public void testSameColumn() {
        StudentDO studentDO = CommonOps.insertOne(dbHelper);

        StudentSameColumnNameVO one = dbHelper.getOne(
                StudentSameColumnNameVO.class, "where name=?", studentDO.getName());

        assert one.getName().endsWith("FFFFFFFF");
        assert one.getName2() == null;
    }

    @Test
    public void testReadIfNull() {
        SchoolDO schoolDO = new SchoolDO();
        schoolDO.setName(null);
        dbHelper.insert(schoolDO);

        SchoolForReadNullDO one = dbHelper.getOne(SchoolForReadNullDO.class, "where id=?", schoolDO.getId());
        assert one.getId().equals(schoolDO.getId());
        assert schoolDO.getName() == null;
        assert one.getName().equals("myname");
    }

    @Test
    public void testVirtualTable() {
        SchoolDO schoolDO = new SchoolDO();
        schoolDO.setName("collageA");
        dbHelper.insert(schoolDO);

        StudentDO studentDO = CommonOps.insertOne(dbHelper);
        studentDO.setSchoolId(schoolDO.getId());
        dbHelper.update(studentDO);

        StudentDO studentDO2 = CommonOps.insertOne(dbHelper);
        studentDO2.setSchoolId(schoolDO.getId());
        dbHelper.update(studentDO2);

        // virtual table SQL
        {
            // get all
            List<StudentVirtualTableVO> all = dbHelper.getAll(StudentVirtualTableVO.class,
                    "and t1.id in (?) order by t1.id",
                    ListUtils.newList(studentDO.getId(), studentDO2.getId()));
            assert all.size() == 2;
            assert all.get(0).getId().equals(studentDO.getId());
            assert all.get(1).getId().equals(studentDO2.getId());
            assert all.get(0).getName().equals(studentDO.getName());
            assert all.get(1).getName().equals(studentDO2.getName());
            assert all.get(0).getSchoolName().equals(schoolDO.getName());
            assert all.get(1).getSchoolName().equals(schoolDO.getName());

            // get page
            PageData<StudentVirtualTableVO> page = dbHelper.getPage(StudentVirtualTableVO.class, 1, 10,
                    "and t1.id in (?) order by t1.id",
                    ListUtils.newList(studentDO.getId(), studentDO2.getId()));
            assert page.getTotal() == 2;
            assert page.getData().size() == 2;
            assert page.getData().get(0).getId().equals(studentDO.getId());
            assert page.getData().get(1).getId().equals(studentDO2.getId());
            assert page.getData().get(0).getName().equals(studentDO.getName());
            assert page.getData().get(1).getName().equals(studentDO2.getName());
            assert page.getData().get(0).getSchoolName().equals(schoolDO.getName());
            assert page.getData().get(1).getSchoolName().equals(schoolDO.getName());
        }

        // virtual table path
        {
            // get all
            List<StudentVirtualTableVO2> all = dbHelper.getAll(StudentVirtualTableVO2.class,
                    "and t1.id in (?) order by t1.id",
                    ListUtils.newList(studentDO.getId(), studentDO2.getId()));
            assert all.size() == 2;
            assert all.get(0).getId().equals(studentDO.getId());
            assert all.get(1).getId().equals(studentDO2.getId());
            assert all.get(0).getName().equals(studentDO.getName());
            assert all.get(1).getName().equals(studentDO2.getName());
            assert all.get(0).getSchoolName().equals(schoolDO.getName());
            assert all.get(1).getSchoolName().equals(schoolDO.getName());

            // get page
            PageData<StudentVirtualTableVO2> page = dbHelper.getPage(StudentVirtualTableVO2.class, 1, 10,
                    "and t1.id in (?) order by t1.id",
                    ListUtils.newList(studentDO.getId(), studentDO2.getId()));
            assert page.getTotal() == 2;
            assert page.getData().size() == 2;
            assert page.getData().get(0).getId().equals(studentDO.getId());
            assert page.getData().get(1).getId().equals(studentDO2.getId());
            assert page.getData().get(0).getName().equals(studentDO.getName());
            assert page.getData().get(1).getName().equals(studentDO2.getName());
            assert page.getData().get(0).getSchoolName().equals(schoolDO.getName());
            assert page.getData().get(1).getSchoolName().equals(schoolDO.getName());
        }

    }

}
