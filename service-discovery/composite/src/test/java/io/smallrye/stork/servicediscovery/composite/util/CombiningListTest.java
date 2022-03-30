package io.smallrye.stork.servicediscovery.composite.util;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CombiningListTest {

    @Test
    void shouldIterateEmpty() {
        CombiningList<String> combiningList = new CombiningList<>(emptyList());
        for (String s : combiningList) {
            fail("No element expected, got " + s);
        }
        assertThat(combiningList.isEmpty()).isTrue();
    }

    @Test
    void shouldIterateListOfEmpty() {
        CombiningList<String> combiningList = new CombiningList<>(asList(emptyList(), emptyList(), emptyList()));
        for (String s : combiningList) {
            fail("No element expected, got " + s);
        }
        assertThat(combiningList.isEmpty()).isTrue();
    }

    @Test
    void shouldIterateSingletonWithEmpty() {
        for (String s : new CombiningList<String>(List.of(emptyList()))) {
            fail("No element expected, got " + s);
        }
    }

    @Test
    void shouldIterateEmptyNonEmptyEmpty() {
        List<String> nonEmpty = new ArrayList<>(asList("a", "b", "c"));
        List<String> actual = new ArrayList<>();

        for (String element : new CombiningList<>(asList(emptyList(), nonEmpty, emptyList()))) {
            actual.add(element);
        }
        assertThat(actual).containsExactlyElementsOf(nonEmpty);
    }

    @Test
    void shouldIterateNonEmptyEmptyNonEmpty() {
        List<String> nonEmpty1 = new ArrayList<>(asList("a", "b", "c"));
        List<String> nonEmpty2 = new ArrayList<>(asList("f", "e", "d"));
        List<String> actual = new ArrayList<>();

        for (String element : new CombiningList<>(asList(nonEmpty1, emptyList(), nonEmpty2))) {
            actual.add(element);
        }
        assertThat(actual).containsExactly("a", "b", "c", "f", "e", "d");
    }

    @Test
    void shouldIterateNonEmptyEmptyEmptyLists() {
        List<String> nonEmpty = new ArrayList<>(asList("a", "b", "c"));
        List<String> actual = new ArrayList<>();

        for (String element : new CombiningList<>(asList(emptyList(), nonEmpty, emptyList()))) {
            actual.add(element);
        }
        assertThat(actual).containsExactlyElementsOf(nonEmpty);
    }

    @Test
    void shouldDoToArray() {
        List<String> nonEmpty1 = new ArrayList<>(asList("a", "b", "c"));
        List<String> nonEmpty2 = new ArrayList<>(asList("f", "e", "d"));
        CombiningList<String> combiningList = new CombiningList<>(asList(nonEmpty1, emptyList(), nonEmpty2, emptyList()));

        Object[] objects = combiningList.toArray();

        assertThat(objects.length).isEqualTo(6);

        String resultAsString = "abcfed";
        for (int i = 0; i < objects.length; i++) {
            assertThat(objects[i]).isEqualTo(String.valueOf(resultAsString.charAt(i)));
        }
    }

    @Test
    void shouldDoToArrayWithArgument() {
        List<String> nonEmpty1 = new ArrayList<>(asList("a", "b", "c"));
        List<String> nonEmpty2 = new ArrayList<>(asList("f", "e", "d"));
        CombiningList<String> combiningList = new CombiningList<>(asList(nonEmpty1, emptyList(), nonEmpty2, emptyList()));

        assertThat(combiningList.size()).isEqualTo(6);

        String[] array = new String[1];
        array = combiningList.toArray(array);

        assertThat(array.length).isEqualTo(6);

        String resultAsString = "abcfed";
        for (int i = 0; i < array.length; i++) {
            assertThat(array[i]).isEqualTo(String.valueOf(resultAsString.charAt(i)));
        }
    }

    @Test
    void shouldDoContainsProperly() {
        List<String> nonEmpty1 = new ArrayList<>(asList("a", "b", "c"));
        List<String> nonEmpty2 = new ArrayList<>(asList("f", "e", "d", "a"));
        CombiningList<String> combiningList = new CombiningList<>(asList(nonEmpty1, emptyList(), nonEmpty2, emptyList()));

        assertThat(combiningList.isEmpty()).isFalse();
        assertThat(combiningList.containsAll(asList("a", "b", "d"))).isTrue();
        assertThat(combiningList.containsAll(asList("a", "b", "z"))).isFalse();
        assertThat(combiningList.contains("z")).isFalse();
        assertThat(combiningList.contains("d")).isTrue();
    }

    @Test
    void shouldDoLastIndexOfProperly() {
        List<String> nonEmpty1 = new ArrayList<>(asList("a", "b", "c"));
        List<String> nonEmpty2 = new ArrayList<>(asList("f", "e", "d", "a"));
        CombiningList<String> combiningList = new CombiningList<>(asList(nonEmpty1, emptyList(), nonEmpty2, emptyList()));

        assertThat(combiningList.lastIndexOf("z")).isEqualTo(-1);
        assertThat(combiningList.lastIndexOf("a")).isEqualTo(6);
        assertThat(combiningList.lastIndexOf("b")).isEqualTo(1);
    }

    @Test
    void shouldGetProperly() {
        List<String> nonEmpty1 = new ArrayList<>(asList("a", "b", "c"));
        List<String> nonEmpty2 = new ArrayList<>(asList("f", "e", "d", "a"));
        CombiningList<String> combiningList = new CombiningList<>(asList(nonEmpty1, emptyList(), nonEmpty2, emptyList()));

        assertThat(combiningList.get(0)).isEqualTo("a");
        assertThat(combiningList.get(1)).isEqualTo("b");
        assertThat(combiningList.get(6)).isEqualTo("a");
        assertThatThrownBy(() -> combiningList.get(7)).isInstanceOf(IndexOutOfBoundsException.class);
    }
}
