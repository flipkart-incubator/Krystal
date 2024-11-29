package com.flipkart.krystal.tags;

import static com.flipkart.krystal.tags.TagKey.namedValueTagKey;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

/**
 * A container for all tags assigned to an krystal graph element, like a facet, logic, vajram/kryon
 * etc.
 */
public final class ElementTags {

  private static final ElementTags EMPTY_TAGS = new ElementTags(ImmutableMap.of());

  private final ImmutableMap<TagKey, Tag<?>> tags;

  private ElementTags(Map<TagKey, Tag<?>> tags) {
    this.tags = ImmutableMap.copyOf(tags);
  }

  public static ElementTags of(Map<TagKey, Tag<?>> tags) {
    return new ElementTags(tags);
  }

  public static ElementTags emptyTags() {
    return EMPTY_TAGS;
  }

  public <A extends Annotation> Optional<A> getAnnotationByType(Class<? extends A> annotationType) {
    return getAnnotationByKey(new TagKey(annotationType, annotationType));
  }

  public Optional<NamedValueTag> getNamedValueTag(String name) {
    return getAnnotationByKey(namedValueTagKey(name));
  }

  public ImmutableCollection<Tag<?>> asCollection() {
    return tags.values();
  }

  public static NamedValueTag namedValueTag(String name, String value) {
    return new NamedValueTag() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String value() {
        return value;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return NamedValueTag.class;
      }
    };
  }

  private <A extends Annotation> Optional<A> getAnnotationByKey(TagKey tagKey) {
    @SuppressWarnings("unchecked")
    Tag<A> tag = (Tag<A>) tags.get(tagKey);
    return Optional.ofNullable(tag).map(Tag::tagValue);
  }
}
