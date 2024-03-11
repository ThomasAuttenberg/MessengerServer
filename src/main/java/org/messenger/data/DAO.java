package org.messenger.data;

import org.messenger.data.interfaces.DBSerializable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

public abstract class DAO<T extends DBSerializable> {

    private final Class<T> class_;
    private final String dataBaseAssociatedTable;

    protected DAO(final Class<T> class_){
        this.class_ = class_;
        org.example.DataBaseTable annotation;
        if((annotation = class_.getDeclaredAnnotation(org.example.DataBaseTable.class)) == null) throw new RuntimeException(
                "No DataBase table for class "+class_.getSimpleName()+". Use @DataBaseTable to it");
        dataBaseAssociatedTable = annotation.name();
    }

    protected final <fieldT> LinkedList<T> getByField(Field field, fieldT fieldValue) throws IllegalAccessException, SQLException {
        return this.loadByField(field,fieldValue);
    }

    public void create(T entity) throws IllegalAccessException, SQLException {
        StringBuilder queryBuilder = new StringBuilder("INSERT INTO "+dataBaseAssociatedTable+"(");
        StringBuilder valuesQueryBuilder = new StringBuilder();
        Field[] fields = class_.getDeclaredFields();
        boolean isFirst = true;
        for(Field field : fields){
            org.example.DataBaseField annotation;
            if((annotation = field.getDeclaredAnnotation(org.example.DataBaseField.class)) != null){
                if(!annotation.isSequence()){
                    if(isFirst){
                        isFirst=false;
                    }else{
                        queryBuilder.append(",");
                        valuesQueryBuilder.append(",");
                    }
                    try {
                        field.setAccessible(true);
                        valuesQueryBuilder.append("'"+field.get(entity)+"'");
                    } catch (IllegalAccessException e) {
                        throw new IllegalAccessException("Impossible to get the access to the " +
                                class_.getSimpleName() + "'s field " + field.getName());
                    }
                    queryBuilder.append(field.getName());
                }
            }
        }
        queryBuilder.append(") VALUES ("+valuesQueryBuilder.toString()+");");
        DataBaseConnection connection = new DataBaseConnection();
        connection.execute(queryBuilder.toString());
    }
    public void delete(T entity) throws SQLException, IllegalAccessException {
        Field[] fields = class_.getDeclaredFields();
        Field primaryKey = null;
        for (Field field : fields) {
            org.example.DataBaseField annotation;
            if (field.isAnnotationPresent(org.example.DataBaseField.class)) {
                annotation = field.getDeclaredAnnotation(org.example.DataBaseField.class);
                if (annotation.isPrimaryKey()) {
                    primaryKey = field;
                    continue;
                }
            }
        }
        if(primaryKey == null) throw new RuntimeException("No primary key field in the class");
        DataBaseConnection connection = new DataBaseConnection();
        primaryKey.setAccessible(true);
        try {
            connection.execute("DELETE FROM "+dataBaseAssociatedTable+" WHERE "+primaryKey.getName()+" = '"+primaryKey.get(entity)+"';");
        } catch (IllegalAccessException e) {
            IllegalAccessException exception = new IllegalAccessException("Impossible to get the access to the " +
                    class_.getSimpleName() + "'s field " + primaryKey.getName());
            exception.setStackTrace(e.getStackTrace());
            throw exception;
        }
    }
    public void update(T entity) throws IllegalAccessException, SQLException {
        Field[] fields = class_.getDeclaredFields();
        Field primaryKey = null;
        StringBuilder query = new StringBuilder("UPDATE " + dataBaseAssociatedTable + " SET ");
        boolean isFirst = true;
        for(Field field : fields){
            org.example.DataBaseField annotation;
            if(field.isAnnotationPresent(org.example.DataBaseField.class)){
                annotation = field.getDeclaredAnnotation(org.example.DataBaseField.class);
                if(annotation.isPrimaryKey()) {
                    primaryKey = field;
                    continue;
                }
                field.setAccessible(true);
                if(isFirst) {
                    isFirst = false;
                }else{
                    query.append(", ");
                }
                query.append(field.getName()).append(" = '").append(field.get(entity)).append("'");
            }
        }
        if(primaryKey == null) throw new RuntimeException("No primary key field in the class");
        primaryKey.setAccessible(true);
        query.append(" WHERE ").append(primaryKey.getName()).append(" = '").append(primaryKey.get(entity)).append("';");
        DataBaseConnection connection = new DataBaseConnection();
        System.out.println(query.toString());
        connection.execute(query.toString());
    }
    protected final <fieldT> LinkedList<T> loadByField(Field field_, fieldT fieldValue) throws SQLException, IllegalAccessException {
        Field[] fields = class_.getDeclaredFields();

        field_.setAccessible(true);
        String query = "SELECT * FROM "+dataBaseAssociatedTable+" WHERE "+field_.getName()+" = '"+fieldValue+"';";
        //System.out.println(query);
        ResultSet set;
        try{
            DataBaseConnection connection = new DataBaseConnection();
            set = connection.executeQuery(query);
        }catch (SQLException e){
            throw new SQLException("DataBase connection is unable");
        }
        LinkedList<T> linkedList =new LinkedList<T>();
        while(set.next()) {
            T entity;
            try {
                linkedList.addLast(class_.getConstructor().newInstance());
                entity = linkedList.getLast();
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalAccessException("Impossible to create new instance of "+class_.getSimpleName() +
                        ".\nCheck it and it's constructor accessibility");
            }
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    //System.out.println(set.);
                    field.set(entity, set.getObject(field.getName()));
                } catch (IllegalAccessException e) {
                    IllegalAccessException exception = new IllegalAccessException("Impossible to get the access to the " +
                            class_.getSimpleName() + "'s field " + field.getName());
                    exception.setStackTrace(e.getStackTrace());
                    throw exception;
                } catch (SQLException e) {
                    throw new SQLException("DataBase reply doesn't contains the coloumn " + field.getName() +
                            "\n while working with class"+class_.getSimpleName()+"\nCheck if class is correct declared");
                }
            }
        }
        return linkedList;
    }

}
