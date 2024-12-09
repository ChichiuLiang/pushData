package org.example.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 将一个集合分段的类
 */
public class ListUtil<T> {
    public static <T> void deepSelfCopy(List<T> deepSelfCopy, List<T> list ){
        if (list.size()==0) {
            return;
        }
        for (int i=1;i<deepSelfCopy.size();i++) {
            deepSelfCopy.remove(deepSelfCopy.get(i));
        }

        if (deepSelfCopy.size()==0) {
            deepSelfCopy.add(list.get(0));
        }
        else {
            deepSelfCopy.set(0,list.get(0));
        }

        for (int i=1;i<list.size();i++) {
            deepSelfCopy.add(list.get(i));
        }
    }

    public static <T> List<T> deepSelfCopy(List<T> list){
        List<T> temp = new ArrayList<>();
        deepSelfCopy(temp,list);
        return temp;
    }

    public static <T extends Serializable> T deepCopy(T obj) {
        if (obj == null) {
            return null;
        }

        try {
            // 创建字节输出流和对象输出流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            // 将原始对象写入对象输出流进行序列化
            oos.writeObject(obj);

            // 创建字节输入流和对象输入流
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);

            // 从对象输入流读取序列化的对象并进行反序列化，得到新的拷贝对象
            T copy = (T) ois.readObject();

            // 关闭流
            oos.close();
            ois.close();

            return copy;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 执行列表的深拷贝
     * @param list 要进行深拷贝的列表
     * @return 深拷贝后的新列表
     */
    public static <T extends Serializable> List<T> deepCopyList(List<T> list) {
        if (list == null) {
            return null;
        }

        List<T> copyList = new ArrayList<>();
        for (T item : list) {
            T copy = deepCopy(item);
            copyList.add(copy);
        }

        return copyList;
    }

    public static <T> boolean isNotNull (List<T>  list){
        return list != null && list.size() > 0 && list.get(0) != null;
    }

    public static <T> boolean isNull (List<T>  list){
        return !(list != null && list.size() > 0 && list.get(0) != null);
    }
}
