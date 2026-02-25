package com.ownwn.server.java.lang.replacement.test;

import com.ownwn.server.java.lang.replacement.*;

public class Tests {

    static void main() {
//        listTest();
//        hashMapTest();
//        hashSetTest();
//        streamTest();
        fileTest();
    }

    static void fileTest() {
        File f = new File("/home/owen/files");
        var res = f.listFiles();
        System.out.println(Arrays.toString(res));


        File sec = new File("/home/owen/files/first");
        var res2 = sec.listFiles();
        System.out.println(Arrays.toString(res2));

        System.out.println(sec.exists());
        System.out.println(new File("FOO/ASDASDASFDSDFSDFAS").exists());

        System.out.println(f.isDirectory());
        System.out.println(sec.isDirectory());
    }

    static void streamTest() {
        List<Integer> l = new ArrayList<>();
        java.util.List<Integer> l2 = new java.util.ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            int random = (int) (100 * Math.random());

            l.add(random);
            l2.add(random);
        }

        List<Integer> l3 = new ArrayList<>();
        l3.add(1);
        l3.add(2);
        l3.add(4);

        List<Integer> l4 = new ArrayList<>();
        l4.add(3);
        l4.add(6);
        l4.add(12);

        if (!l3.stream().map(i -> i*3).toList().equals(l4)) {
            throw new Error("bad map");
        }

        if (!l.stream().toList().equals(l)) {
            throw new Error("bad toList \n" + l + "\n" + l.stream().toList());
        }


    }

    static void hashSetTest() {
        Set<Integer> set = new HashSet<>();
        java.util.Set<Integer> set2 = new java.util.HashSet<>();


        for (int i = 0; i < 1000000; i++) {
            int random = (int) (100 * Math.random());

            set.add(random);
            set2.add(random);

            if (set.size() != set2.size()) {
                throw new Error("size");
            }

            if (!set.equals(set2)) {
                    throw new Error("equals\n" + set + " " + set2);
            }

            if (Math.random() < 0.5) {
                set.remove(random+1);
                set2.remove(random+1);
            }

        }
    }

    static void listTest() {
        List<Integer> l = new ArrayList<>();
        java.util.List<Integer> l2 = new java.util.ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            int random = (int) (100 * Math.random());

            l.add(random);
            l2.add(random);

            if (l.size() != l2.size()) {
                throw new Error("size");
            }

            if (!l.equals(l2) || !l.equals(l)) {
                throw new Error("equals");
            }

            if (Math.random() < 0.5) {
                int index = (int) (Math.random() * l.size());
                l.remove(index);
                l2.remove(index);
            }

            if (Math.random() < 0.3) {
                int index = (int) (Math.random() * l.size());
                int rann = (int) (Math.random() * l.size());
                l.add(index, rann);
                l2.add(index, rann);
            }
        }

    }

    static void hashMapTest() {
        Map<Integer, Integer> map = new HashMap<>();
        java.util.Map<Integer, Integer> map2 = new java.util.HashMap<>();


        for (int i = 0; i < 1000000; i++) {
            int randomKey = (int) (100 * Math.random());
            int randomValue = (int) (100 * Math.random());
            map.put(randomKey, randomValue);
            map2.put(randomKey, randomValue);

            if (map.containsKey(randomKey) != map2.containsKey(randomKey)) throw new Error();
            if (map.containsValue(randomValue) != map2.containsValue(randomValue)) throw new Error();

            var r1 = map.remove(randomKey);
            var r2 = map2.remove(randomKey);
            if (!r1.equals(r2)) {
                throw new Error("remove " + r1 + " " + r2);
            }
//            System.out.println(map.size() + " " + map2.size());
            if (map.size() != map2.size()) {
                throw new Error(map.size() + " " + map2.size() + "\n" + "\n" + map + "\n" + map2);
            }
//            if (map.toString().intern() != map2.toString().intern()) throw new Error("\n" + map + "\n" + map2);
        }
    }
}
