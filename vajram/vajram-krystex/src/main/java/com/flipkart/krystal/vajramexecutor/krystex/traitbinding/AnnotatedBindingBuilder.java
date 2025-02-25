package com.flipkart.krystal.vajramexecutor.krystex.traitbinding;

import com.flipkart.krystal.vajram.VajramTraitDef;
import com.flipkart.krystal.vajramexecutor.krystex.traitbinding.Anno.AnnoClass;
import com.flipkart.krystal.vajramexecutor.krystex.traitbinding.Anno.AnnoObject;
import java.lang.annotation.Annotation;

public class AnnotatedBindingBuilder extends LinkedBindingBuilder {

  private final Class<? extends VajramTraitDef<?>> traitDef;

  public AnnotatedBindingBuilder(Class<? extends VajramTraitDef<?>> traitDef) {
    super(traitDef, new Anno.Unqualified());
    this.traitDef = traitDef;
  }

  public LinkedBindingBuilder annotatedWith(Annotation annotation) {
    return new LinkedBindingBuilder(traitDef, new AnnoObject(annotation));
  }

  public LinkedBindingBuilder annotatedWith(Class<? extends Annotation> annotation) {
    return new LinkedBindingBuilder(traitDef, new AnnoClass(annotation));
  }
}
