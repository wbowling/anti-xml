package com.codecommit.antixml

/*
 * The URI is required and cannot be an empty namespace name. See "2.2 Use of URIs as Namespace Names", "The empty
 * string, though it is a legal URI reference, cannot be used as a namespace name.".
 *
 * TODO: I wonder, should NB be exactly like a linked list that always end with an EmptyNB? That way there won't be
  * any need for any options probably giving significant performance savings when matching and extracting.
  *
  * TODO: Implement javax.xml.namespace.NamespaceContext#getPrefixes. Lists all the prefixes *this* URI is bound to.
  * See http://docs.oracle.com/javase/6/docs/api/javax/xml/namespace/NamespaceContext.html#getPrefixes(java.lang.String)
 */
sealed trait NamespaceBinding {
  def uri: Option[String]

  def parent: Option[NamespaceBinding]

  def isEmpty: Boolean

  def append(uri: String): NamespaceBinding = new UnprefixedNamespaceBinding(uri, Some(this))

  def append(prefix: String, uri: String): NamespaceBinding = new PrefixedNamespaceBinding(prefix, uri, Some(this))

  def findByPrefix(prefix: String): Option[NamespaceBinding] = this match {
    case UnprefixedNamespaceBinding(_, _) if(prefix.isEmpty) => Some(this)
    case UnprefixedNamespaceBinding(_, Some(parent)) => parent.findByPrefix(prefix)
    case UnprefixedNamespaceBinding(_, None) => None
    case PrefixedNamespaceBinding(p, _, Some(parent)) => if(p.equals(prefix)) Some(this) else parent.findByPrefix(prefix)
    case PrefixedNamespaceBinding(p, _, None) => if(p.equals(prefix)) Some(this) else None
    case EmptyNamespaceBinding => None
  }

  def findByUri(uri: String): Option[NamespaceBinding] = {
    if(this.uri.filter(uri ==).isDefined)
      Some(this)
    else
      parent.flatMap(_.findByUri(uri))
  }

  /**
   * This is probably not a good idea.
   */
  private[antixml] def noParent: NamespaceBinding

  def toList: List[NamespaceBinding] = {
    def toStream(nb: NamespaceBinding): Stream[NamespaceBinding] = Stream.cons(nb.noParent, if(nb.parent.isDefined) toStream(nb.parent.get) else Stream.empty)
    toStream(this).toList
  }
}

object NamespaceBinding {
  val empty: NamespaceBinding = EmptyNamespaceBinding

  def apply(t: (String, String)) = new PrefixedNamespaceBinding(t._1, t._2, None)

  def apply(t: (String, String), parent: NamespaceBinding) = new PrefixedNamespaceBinding(t._1, t._2, Some(parent))

  def apply(prefix: String,  uri: String, parent: NamespaceBinding) = new PrefixedNamespaceBinding(prefix, uri, Some(parent))

  def apply(prefix: String,  uri: String) = new PrefixedNamespaceBinding(prefix, uri, None)

  def apply(uri: String, parent: NamespaceBinding) = new UnprefixedNamespaceBinding(uri, Some(parent))

  def apply(uri: String) = new UnprefixedNamespaceBinding(uri, None)
}

object NamespaceUri {
  def unapply(namespaceBinding: NamespaceBinding) = namespaceBinding.uri
}

sealed class NSRepr(val uri: String)

object NSRepr {
  def apply(uri: String): NSRepr = new NSRepr(uri)
  def apply(nb: NamespaceBinding): NSRepr = new NSRepr(nb.uri.getOrElse(throw new IllegalArgumentException("A namespace binding has to have an URI")))
}

private [antixml]case object EmptyNamespaceBinding extends NamespaceBinding {
  def uri = None
  def parent = None
  def isEmpty = true

  override def append(uri: String): NamespaceBinding = new UnprefixedNamespaceBinding(uri, None)

  override def append(prefix: String, uri: String): NamespaceBinding = new PrefixedNamespaceBinding(prefix, uri, None)

  private[antixml] def noParent = this

  override def toList = List.empty
}

case class PrefixedNamespaceBinding(prefix: String, _uri: String,  override val parent: Option[NamespaceBinding] = None) extends NamespaceBinding {
  if (!Elem.isValidName(prefix)) {
    throw new IllegalArgumentException("Illegal namespace prefix, '" + prefix + "'")
  }
  if (!Elem.isValidNamespaceUri(_uri)) {
    throw new IllegalArgumentException("Illegal namespace uri, '" + _uri + "'")
  }

  def uri = Some(_uri)
  def isEmpty = false
  private[antixml] def noParent = PrefixedNamespaceBinding(prefix, _uri, None)
}

case class UnprefixedNamespaceBinding(_uri: String,  override val parent: Option[NamespaceBinding] = None) extends NamespaceBinding {
  if (!Elem.isValidNamespaceUri(_uri)) {
    throw new IllegalArgumentException("Illegal namespace uri, '" + _uri + "'")
  }

  def uri = Some(_uri)
  def isEmpty = false
  private[antixml] def noParent = UnprefixedNamespaceBinding(_uri, None)
}
