package oo_db.db

import java.io.{File, RandomAccessFile}

import scalaz.std.option.optionSyntax._
import oo_db.db.nodes._

import scala.annotation.tailrec

class BTree(private val io: IoManager) {
	
	// =====| General |=====
	
	def close: Unit =
		io.close
	
	def showStats: Unit =
		io.showStats
	
	// =====| Operations |=====
	
	def get(key: Long): Option[Long] = {
		@tailrec
		def find(depth: Int, seek: Long): Option[Long] =
			if (depth < io.getHeight)
				find(depth + 1, io.readInternalNode(seek).childPos(key))
			else
				io.readLeafNode(seek).find(key)
		
		if (io.getHeight == 0)
			None
		else
			find(1, io.getRoot)
	}
	
	def insert(key: Long, value: Long): Unit = {
		// Helpers
		def handleInsert(res: Option[(Node, Option[(Long, Node)])]): Option[(Long, Long)] = res match {
			case None =>
				None
			case Some((n1, None)) =>
				io.writeNode(n1)
				None
			case Some((n1, Some((minKey, n2)))) =>
				io.writeNode(n1)
				io.insertNewNode(n2)
				(minKey, n2.pos).some
		}
		
		def loop(depth: Int, seek: Long): Option[(Long, Long)] =
			if (depth < io.getHeight) {
				val internalNode: InternalNode = io.readInternalNode(seek)
				loop(depth + 1, internalNode.childPos(key)) match {
					case None =>
						None
					case Some((newKey, newChild)) =>
						handleInsert(
							internalNode.insert(io.getOrder, io.nextFreePos, newKey, newChild)
						)
				}
			}
			else {
				handleInsert(
					io.readLeafNode(seek).insert(io.getOrder, io.nextFreePos, key, value)
				)
			}
		
		// Action
		if (io.getHeight > 0)
			loop(1, io.getRoot) match {
				case None =>
				case Some((rightMin, pos)) =>
					val newRoot: InternalNode = InternalNode(io.nextFreePos, List(rightMin), List(io.getRoot, pos))
					io.insertRootNode(newRoot)
			}
		else {
			val node: LeafNode = new LeafNode(io.nextFreePos, List(key), List(value), 0L)
			io.insertRootNode(node)
		}
	}
	
	def remove(key: Long): Option[Long] = {
		// Helpers
		/**
		  * None =>
		  * 	No deletion
		  * Some((v, None, None)) =>
		  * 	deleted key, which pointed to v
		  * 	at this point in the stack, all merging has been handled
		  * 	also, no keys need to be changed
		  * Some((v, Some(n), None)) =>
		  * 	deleted key, which pointed to v
		  * 	after deletion cascaded from child, it is now too small, and needs to be handled
		  * 	also, no keys need to be changed
		  * Some((v, None, Some(k)) =>
		  * 	deleted key, which pointed to v
		  * 	at this point in the stack, all merging has been handled
		  * 	key was the smallest in its leaf, and needs a new reference internally
		  * 		this
		  * Some((v, Some(n), Some(k)) =>
		  * 	both previous cases need to be handled
		  */
		def loop(depth: Int, seek: Long): Option[(Long, Option[Node], Option[Long])] = {
		
		}
		
		
		// Action
		if (io.getHeight > 0)
			loop(1, io.getRoot) match {
				case None =>
					None
				case Some((v, None, None)) =>
					v.some
				case Some((v, Some(n), _)) =>
					
					v.some
				case Some((v, None, Some(k))) =>
					
					v.some
			}
		else
			None
	}
	
}

object BTree {
	// Constants
	val MAGIC_NUMBER: Int = 28366439
	val MIN_ORDER: Int = 3
	
	val MAGIC_NUMBER_POSITION: Long = 0L // Int
	val ORDER_POSITION: Long = 4L // Int
	val HEIGHT_POSITION: Long = 8L // Int
	val ROOT_POSITION: Long = 12L // Long
	val FREE_LIST_POSITION: Long = 20L // Long
	
	val FREE_LIST_MARKER: Byte = 0
	val INTERNAL_NODE_MARKER: Byte = 1
	val LEAF_NODE_MARKER: Byte = 2
	
	// ...
	
	def create(order: Int, path: String): BTree = {
		if (order < MIN_ORDER)
			throw new IllegalArgumentException(s"order $order is less than min $MIN_ORDER")
		
		val file: File = new File(path)
		if (file.exists())
			throw new IllegalArgumentException(s"File already exists at path $path")
		
		file.getParentFile.mkdirs
		
		val bTreeFile: RandomAccessFile = new RandomAccessFile(file, "rw")
		bTreeFile.writeInt(MAGIC_NUMBER) // Magic Number
		bTreeFile.writeInt(order) // Order
		bTreeFile.writeInt(0) // Height
		bTreeFile.writeLong(0L) // Root
		bTreeFile.writeLong(0L) // Free List
		bTreeFile.close
		
		new BTree(new IoManager(path))
	}
	
	def load(path: String): BTree = {
		val file: File = new File(path)
		if (!file.exists())
			throw new IllegalArgumentException(s"File does not exist at path $path")
		
		new BTree(new IoManager(path))
	}
	
}
