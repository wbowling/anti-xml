package com.codecommit.antixml

import annotation.tailrec

/*
 * The URI is required and cannot be an empty namespace name. See "2.2 Use of URIs as Namespace Names", "The empty
 * string, though it is a legal URI reference, cannot be used as a namespace name.".
 *
 */
sealed trait NamespaceBinding {
  def uri: Option[String]

  def parent: NamespaceBinding

  def isEmpty: Boolean

  def append(uri: String): NamespaceBinding = new UnprefixedNamespaceBinding(uri, this)

  def append(prefix: String, uri: String): NamespaceBinding = new PrefixedNamespaceBinding(prefix, uri, this)

  @tailrec final def findByPrefix(prefix: String): Option[NamespaceBinding] = this match {
    case EmptyNamespaceBinding => None
    case UnprefixedNamespaceBinding(_, parent) => if(prefix.isEmpty) Some(this) else parent.findByPrefix(prefix)
    case PrefixedNamespaceBinding(p, _, parent) => if(p == prefix) Some(this) else parent.findByPrefix(prefix)
  }

  // If prefix is supplied, then an UnprefixedNamespaceBinding will never be returned
  @tailrec final def findByUri(uri: String, prefix: String=""): Option[NamespaceBinding] = this match {
      case EmptyNamespaceBinding => None
      case PrefixedNamespaceBinding(p, u, parent) => if (u == uri && (prefix.isEmpty() || p==prefix)) Some(this) else parent.findByUri(uri, prefix)
      case UnprefixedNamespaceBinding(u, parent) => if (prefix.isEmpty() && u == uri) Some(this) else parent.findByUri(uri, prefix)
    }

  def findPrefixes(uri: String): List[String] = {
    @tailrec def find(nb: NamespaceBinding, list: List[String]): List[String] = nb match {
      case EmptyNamespaceBinding => list
      case UnprefixedNamespaceBinding(u, parent) => find(parent, if(u == uri) "" :: list else list)
      case PrefixedNamespaceBinding(p, u, parent) => find(parent, if(u == uri) p :: list else list)
    }
    find(this, Nil)
  }


  /**
   * This is probably not a good idea.
   */
  private[antixml] def noParent: NamespaceBinding

  def toList: List[NamespaceBinding] = {
    @tailrec def toList(nb: NamespaceBinding, list: List[NamespaceBinding]): List[NamespaceBinding] = nb match {
      case EmptyNamespaceBinding => list
      case binding => toList(binding.parent, binding.noParent :: list)
    }
    toList(this, Nil).reverse
  }
}

object NamespaceBinding {
  val empty: NamespaceBinding = EmptyNamespaceBinding

  def apply(t: (String, String)) = new PrefixedNamespaceBinding(t._1, t._2, empty)

  def apply(t: (String, String), parent: NamespaceBinding) = new PrefixedNamespaceBinding(t._1, t._2, parent)

  def apply(prefix: String,  uri: String, parent: NamespaceBinding) = new PrefixedNamespaceBinding(prefix, uri, parent)

  def apply(prefix: String,  uri: String) = new PrefixedNamespaceBinding(prefix, uri, empty)

  def apply(uri: String, parent: NamespaceBinding) = new UnprefixedNamespaceBinding(uri, parent)

  def apply(uri: String) = new UnprefixedNamespaceBinding(uri, empty)
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
  def parent = throw new IllegalStateException("No parent for empty")
  def isEmpty = true

  private[antixml] def noParent = this

  override def toList = List.empty
}

case class PrefixedNamespaceBinding(prefix: String, _uri: String, override val parent: NamespaceBinding = NamespaceBinding.empty) extends NamespaceBinding {
  if (!Elem.isValidName(prefix)) {
    throw new IllegalArgumentException("Illegal namespace prefix, '" + prefix + "'")
  }
  if (!Elem.isValidNamespaceUri(_uri)) {
    throw new IllegalArgumentException("Illegal namespace uri, '" + _uri + "'")
  }

  def uri = Some(_uri)
  def isEmpty = false
  private[antixml] def noParent = PrefixedNamespaceBinding(prefix, _uri, NamespaceBinding.empty)
}

case class UnprefixedNamespaceBinding(_uri: String,  override val parent: NamespaceBinding = NamespaceBinding.empty) extends NamespaceBinding {
  if (!Elem.isValidNamespaceUri(_uri)) {
    throw new IllegalArgumentException("Illegal namespace uri, '" + _uri + "'")
  }

  def uri = Some(_uri)
  def isEmpty = false
  private[antixml] def noParent = UnprefixedNamespaceBinding(_uri, NamespaceBinding.empty)
}

object ElemNamespaceUri {
  def unapply(elem: Elem) = elem.namespaces.findByPrefix(elem.prefix.getOrElse("")).flatMap(_.uri)
}
