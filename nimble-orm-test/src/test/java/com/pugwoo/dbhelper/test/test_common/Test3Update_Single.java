package com.pugwoo.dbhelper.test.test_common;

import com.pugwoo.dbhelper.DBHelper;
import com.pugwoo.dbhelper.annotation.Column;
import com.pugwoo.dbhelper.exception.CasVersionNotMatchException;
import com.pugwoo.dbhelper.test.entity.CasVersionDO;
import com.pugwoo.dbhelper.test.entity.CasVersionLongDO;
import com.pugwoo.dbhelper.test.entity.StudentDO;
import com.pugwoo.dbhelper.test.utils.CommonOps;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class Test3Update_Single {

    @Autowired
    private DBHelper dbHelper;

    @Test
    public void testUpdateNull() {
        StudentDO db = CommonOps.insertOne(dbHelper);
        db.setAge(null);
        dbHelper.updateWithNull(db);

        db = dbHelper.getByKey(StudentDO.class, db.getId());
        assert db.getAge() == null;

        db.setAge(3);
        dbHelper.update(db);
        db.setAge(null);
        assert dbHelper.updateWithNull(db, "where age=?", 3) == 1;

        db = dbHelper.getByKey(StudentDO.class, db.getId());
        assert db.getAge() == null;
    }

    @Test
    public void testUpdate() {
        StudentDO db = CommonOps.insertOne(dbHelper);
        db.setName("nick2");
        dbHelper.update(db);

        db = dbHelper.getByKey(StudentDO.class, db.getId());
        assert db.getName().equals("nick2");

        db.setAge(3);
        dbHelper.update(db);
        db.setAge(null);
        dbHelper.update(db);

        db = dbHelper.getByKey(StudentDO.class, db.getId());
        assert db.getAge().equals(3);

        db.setName("nick3");
        dbHelper.update(db, "where age=?", 3);
        db = dbHelper.getByKey(StudentDO.class, db.getId());
        assert db.getName().equals("nick3");

        List<StudentDO> list = new ArrayList<StudentDO>();
        list.add(db);
        db.setName("nick4");
        dbHelper.update(list);
        db = dbHelper.getByKey(StudentDO.class, db.getId());
        assert "nick4".equals(db.getName());

        // 测试异常参数
        assert dbHelper.update(null) == 0;
        assert dbHelper.update(new ArrayList<>()) == 0;
    }

    @Test
    public void testCasVersion() {
        CasVersionDO casVersionDO = new CasVersionDO();
        casVersionDO.setName("nick");

        assert dbHelper.insert(casVersionDO) > 0; // 插入时会自动写入casVersion字段的值

        assert casVersionDO.getId() > 0;
        assert casVersionDO.getVersion() > 0;

        casVersionDO.setName("nick2");
        assert dbHelper.update(casVersionDO) > 0; // 更新时会自动改casVersion字段的值

        casVersionDO.setName("nick3");
        assert dbHelper.update(casVersionDO) > 0;

        // version设置为null会异常
        casVersionDO.setVersion(null);
        boolean exOccur = false;
        try {
            casVersionDO.setName("nick3");
            dbHelper.update(casVersionDO);
        } catch (Exception e) {
            if(e instanceof CasVersionNotMatchException) {
                exOccur = true;
            }
        }
        assert exOccur;

        // version设置为一个错的值，会异常
        casVersionDO.setVersion(99);
        exOccur = false;
        try {
            casVersionDO.setName("nick3");
            dbHelper.update(casVersionDO);
        } catch (Exception e) {
            if(e instanceof CasVersionNotMatchException) {
                exOccur = true;
            }
        }
        assert exOccur;

        // 再把version设置为3，就正常了
        casVersionDO.setVersion(3);
        assert dbHelper.update(casVersionDO) > 0;

        // 反查之后，版本应该就是4了
        CasVersionDO tmp = dbHelper.getByKey(CasVersionDO.class, casVersionDO.getId());
        assert tmp.getVersion().equals(4);

        assert dbHelper.updateCustom(tmp, "name=?", "nick5") > 0;
        assert dbHelper.updateCustom(tmp, "name=?", "nick6") > 0;
        assert dbHelper.updateCustom(tmp, "name=?", "nick7") > 0;

        // 此时版本应该是7
        tmp = dbHelper.getByKey(CasVersionDO.class, casVersionDO.getId());
        assert tmp.getVersion().equals(7);


        // 测试CAS版本字段是Long的情况
        CasVersionLongDO casVersionLongDO = new CasVersionLongDO();
        casVersionLongDO.setName("nick");

        assert dbHelper.insert(casVersionLongDO) > 0; // 插入时会自动写入casVersion字段的值

        assert casVersionLongDO.getId() > 0;
        assert casVersionLongDO.getVersion() == 1;

        casVersionLongDO.setName("nick2");
        assert dbHelper.update(casVersionLongDO) > 0; // 更新时会自动改casVersion字段的值
        assert casVersionLongDO.getVersion() == 2;

    }

    /**测试更新全是key的DO，实际上就等同于该DO不需要更新，修改条数为0*/
    @Test
    public void testUpdateAllKey() {
        StudentDO studentDO = CommonOps.insertOne(dbHelper);

        AllKeyStudentDO s = new AllKeyStudentDO();
        s.setId(studentDO.getId());

        assert dbHelper.update(s) == 0;
    }

    @Data
    public static class AllKeyStudentDO {
        @Column(value = "id", isKey = true, isAutoIncrement = true)
        private Long id;
    }

}
