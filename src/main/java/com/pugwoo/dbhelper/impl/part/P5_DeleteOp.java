package com.pugwoo.dbhelper.impl.part;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.pugwoo.dbhelper.DBHelperInterceptor;
import com.pugwoo.dbhelper.annotation.Column;
import com.pugwoo.dbhelper.exception.InvalidParameterException;
import com.pugwoo.dbhelper.exception.MustProvideConstructorException;
import com.pugwoo.dbhelper.exception.NotAllowQueryException;
import com.pugwoo.dbhelper.exception.NullKeyValueException;
import com.pugwoo.dbhelper.sql.SQLUtils;
import com.pugwoo.dbhelper.utils.DOInfoReader;

public abstract class P5_DeleteOp extends P4_InsertOrUpdateOp {
	
	/////// 拦截器
	protected void doInterceptBeforeDelete(Class<?> clazz, Object t) {
		List<Object> list = new ArrayList<Object>();
		list.add(t);
		doInterceptBeforeDelete(clazz, list);
	}
	protected <T> void doInterceptBeforeDelete(Class<?> clazz, List<T> list) {
		for (DBHelperInterceptor interceptor : interceptors) {
			boolean isContinue = interceptor.beforeDelete(clazz, list);
			if (!isContinue) {
				throw new NotAllowQueryException("interceptor class:" + interceptor.getClass());
			}
		}
	}
	protected void doInterceptBeforeDelete(Class<?> clazz, String sql, Object[] args) {
		for (DBHelperInterceptor interceptor : interceptors) {
			boolean isContinue = interceptor.beforeDeleteCustom(clazz, sql, args);
			if (!isContinue) {
				throw new NotAllowQueryException("interceptor class:" + interceptor.getClass());
			}
		}
	}
	
	protected void doInterceptAfterDelete(Class<?> clazz, Object t, int rows) {
		List<Object> list = new ArrayList<Object>();
		list.add(t);
		doInterceptAfterDelete(clazz, list, rows);
	}
	protected <T> void doInterceptAfterDelete(Class<?> clazz, List<T> list, int rows) {
		for (int i = interceptors.size() - 1; i >= 0; i--) {
			interceptors.get(i).afterDelete(clazz, list, rows);
		}
	}
	protected void doInterceptAfterDelete(Class<?> clazz, String sql, Object[] args, int rows) {
		for (int i = interceptors.size() - 1; i >= 0; i--) {
			interceptors.get(i).afterDeleteCustom(clazz, sql, args, rows);
		}
	}
	///////////

	@Override
	public <T> int deleteByKey(T t) throws NullKeyValueException {
		Field softDelete = DOInfoReader.getSoftDeleteColumn(t.getClass());
		
		List<Object> values = new ArrayList<Object>();
		String sql = null;
		
		if(softDelete == null) { // 物理删除
			sql = SQLUtils.getDeleteSQL(t, values);
		} else { // 软删除
			Column softDeleteColumn = softDelete.getAnnotation(Column.class);
			sql = SQLUtils.getSoftDeleteSQL(t, softDeleteColumn, values);
		}

		doInterceptBeforeDelete(t.getClass(), t);
		int rows = jdbcExecuteUpdate(sql, values.toArray());
		doInterceptAfterDelete(t.getClass(), t, rows);
		
		return rows;
	}
		
	@Override
	public <T> int deleteByKey(Class<T> clazz, Object keyValue) 
			throws NullKeyValueException, MustProvideConstructorException {
		if(keyValue == null) {
			throw new NullKeyValueException();
		}

		Field keyField = DOInfoReader.getOneKeyColumn(clazz);
		
		try {
			T t = (T) clazz.newInstance();
			DOInfoReader.setValue(keyField, t, keyValue);
			return deleteByKey(t);
		} catch (InstantiationException e) {
			throw new MustProvideConstructorException();
		} catch (IllegalAccessException e) {
			throw new MustProvideConstructorException();
		}
	}
	
	@Override
	public <T> int delete(Class<T> clazz, String postSql, Object... args) {
		if(postSql == null || postSql.trim().isEmpty()) { // warning: very dangerous
			// 不支持缺省条件来删除。如果需要全表删除，请直接运维人员truncate表。
			throw new InvalidParameterException(); 
		}
		
		Field softDelete = DOInfoReader.getSoftDeleteColumn(clazz); // 支持软删除

		String sql = null;
		if(softDelete == null) { // 物理删除
			sql = SQLUtils.getCustomDeleteSQL(clazz, postSql);
		} else { // 软删除
			sql = SQLUtils.getCustomSoftDeleteSQL(clazz, postSql);
		}

		doInterceptBeforeDelete(clazz, sql, args);
		int rows = namedJdbcExecuteUpdate(sql, args);
		doInterceptAfterDelete(clazz, sql, args, rows);
		
		return rows;
	}
	
}
