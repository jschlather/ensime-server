package org.ensime.test

import org.ensime.model._
import org.ensime.protocol._
import org.ensime.protocol.swank.{ SwankProtocolConversions, ReplConfig }
import org.ensime.server._
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.ensime.util._

class SwankProtocolConversionsSpec extends FunSpec with Matchers {

  import SwankTestData._

  describe("SwankProtocolConversionsSpec") {
    TestUtil.withActorSystem { as =>

      import SwankProtocolConversions._
      it("should encode all message types correctly") {
        assert(toWF(SendBackgroundMessageEvent(1, Some("ABCDEF"))).toWireString === """(:background-message 1 "ABCDEF")""")
        assert(toWF(SendBackgroundMessageEvent(1, None)).toWireString === "(:background-message 1 nil)")
        assert(toWF(AnalyzerReadyEvent).toWireString === "(:compiler-ready)")
        assert(toWF(FullTypeCheckCompleteEvent).toWireString === "(:full-typecheck-finished)")
        assert(toWF(IndexerReadyEvent).toWireString === "(:indexer-ready)")

        assert(toWF(NewJavaNotesEvent(NoteList(full = false,
          List(new Note("foo.scala", "testMsg", 1, 50, 55, 77, 5))))).toWireString === """(:java-notes (:is-full nil :notes ((:severity warn :msg "testMsg" :beg 50 :end 55 :line 77 :col 5 :file "foo.scala"))))""")
        assert(toWF(NewScalaNotesEvent(NoteList(full = false,
          List(new Note("foo.scala", "testMsg", 1, 50, 55, 77, 5))))).toWireString === """(:scala-notes (:is-full nil :notes ((:severity warn :msg "testMsg" :beg 50 :end 55 :line 77 :col 5 :file "foo.scala"))))""")

        assert(toWF(ClearAllScalaNotesEvent).toWireString === "(:clear-all-scala-notes)")
        assert(toWF(ClearAllJavaNotesEvent).toWireString === "(:clear-all-java-notes)")

        // toWF(evt: DebugEvent): WireFormat
        assert(toWF(DebugOutputEvent("XXX")).toWireString === """(:debug-event (:type output :body "XXX"))""")
        assert(toWF(DebugStepEvent(207L, "threadNameStr", sourcePos1)).toWireString ===
          """(:debug-event (:type step :thread-id "207" :thread-name "threadNameStr" :file """ + file1_str + """ :line 57))""")
        assert(toWF(DebugBreakEvent(209L, "threadNameStr", sourcePos1)).toWireString ===
          """(:debug-event (:type breakpoint :thread-id "209" :thread-name "threadNameStr" :file """ + file1_str + """ :line 57))""")
        assert(toWF(DebugVMDeathEvent()).toWireString === """(:debug-event (:type death))""")
        assert(toWF(DebugVMStartEvent()).toWireString === """(:debug-event (:type start))""")
        assert(toWF(DebugVMDisconnectEvent()).toWireString === """(:debug-event (:type disconnect))""")

        assert(toWF(DebugExceptionEvent(33L, 209L, "threadNameStr", Some(sourcePos1))).toWireString ===
          """(:debug-event (:type exception :exception "33" :thread-id "209" :thread-name "threadNameStr" :file """ + file1_str + """ :line 57))""")

        assert(toWF(DebugExceptionEvent(33L, 209L, "threadNameStr", None)).toWireString ===
          """(:debug-event (:type exception :exception "33" :thread-id "209" :thread-name "threadNameStr" :file nil :line nil))""")

        assert(toWF(DebugThreadStartEvent(907L)).toWireString === """(:debug-event (:type threadStart :thread-id "907"))""")
        assert(toWF(DebugThreadDeathEvent(907L)).toWireString === """(:debug-event (:type threadDeath :thread-id "907"))""")

        // toWF(obj: DebugLocation)
        assert(toWF(DebugObjectReference(57L).asInstanceOf[DebugLocation]).toWireString ===
          """(:type reference :object-id "57")""")
        assert(toWF(DebugArrayElement(58L, 2).asInstanceOf[DebugLocation]).toWireString ===
          """(:type element :object-id "58" :index 2)""")
        assert(toWF(DebugObjectField(58L, "fieldName").asInstanceOf[DebugLocation]).toWireString ===
          """(:type field :object-id "58" :field "fieldName")""")
        assert(toWF(DebugStackSlot(27L, 12, 23).asInstanceOf[DebugLocation]).toWireString ===
          """(:type slot :thread-id "27" :frame 12 :offset 23)""")

        // toWF(obj: DebugValue)

        // toWF(evt: DebugPrimitiveValue)
        assert(toWF(DebugPrimitiveValue("summaryStr", "typeNameStr").asInstanceOf[DebugValue]).toWireString ===
          """(:val-type prim :summary "summaryStr" :type-name "typeNameStr")""")

        // toWF(evt: DebugClassField)
        assert(toWF(debugClassField).toWireString === """(:index 19 :name "nameStr" :summary "summaryStr" :type-name "typeNameStr")""")

        // toWF(obj: DebugStringInstance)
        assert(toWF(DebugStringInstance("summaryStr", List(debugClassField), "typeNameStr", 5L).asInstanceOf[DebugValue]).toWireString ===
          """(:val-type str :summary "summaryStr" :fields ((:index 19 :name "nameStr" :summary "summaryStr" :type-name "typeNameStr")) :type-name "typeNameStr" :object-id "5")""")

        // toWF(evt: DebugObjectInstance)
        assert(toWF(DebugObjectInstance("summaryStr", List(debugClassField), "typeNameStr", 5L).asInstanceOf[DebugValue]).toWireString ===
          """(:val-type obj :fields ((:index 19 :name "nameStr" :summary "summaryStr" :type-name "typeNameStr")) :type-name "typeNameStr" :object-id "5")""")

        // toWF(evt: DebugNullValue)
        assert(toWF(DebugNullValue("typeNameStr").asInstanceOf[DebugValue]).toWireString ===
          """(:val-type null :type-name "typeNameStr")""")

        // toWF(evt: DebugArrayInstance)
        assert(toWF(DebugArrayInstance(3, "typeName", "elementType", 5L).asInstanceOf[DebugValue]).toWireString ===
          """(:val-type arr :length 3 :type-name "typeName" :element-type-name "elementType" :object-id "5")""")

        // toWF(evt: DebugStackLocal)
        assert(toWF(debugStackLocal1).toWireString === """(:index 3 :name "name1" :summary "summary1" :type-name "type1")""")

        // toWF(evt: DebugStackFrame)
        assert(toWF(debugStackFrame).toWireString === """(:index 7 :locals ((:index 3 :name "name1" :summary "summary1" :type-name "type1") (:index 4 :name "name2" :summary "summary2" :type-name "type2")) :num-args 4 :class-name "class1" :method-name "method1" :pc-location (:file """ + file1_str + """ :line 57) :this-object-id "7")""")

        // toWF(evt: DebugBacktrace)
        assert(toWF(DebugBacktrace(List(debugStackFrame), 17, "thread1")).toWireString === """(:frames ((:index 7 :locals ((:index 3 :name "name1" :summary "summary1" :type-name "type1") (:index 4 :name "name2" :summary "summary2" :type-name "type2")) :num-args 4 :class-name "class1" :method-name "method1" :pc-location (:file """ + file1_str + """ :line 57) :this-object-id "7")) :thread-id "17" :thread-name "thread1")""")

        // toWF(pos: LineSourcePosition)
        assert(toWF(sourcePos1).toWireString === """(:file """ + file1_str + """ :line 57)""")
        // toWF(pos: EmptySourcePosition)
        assert(toWF(sourcePos3).toWireString === "t")
        // toWF(pos: OffsetSourcePosition)
        assert(toWF(sourcePos4).toWireString === """(:file """ + file1_str + """ :offset 456)""")

        // toWF(bp: Breakpoint)
        assert(toWF(breakPoint1).toWireString === """(:file """ + file1_str + """ :line 57)""")

        // toWF(config: BreakpointList)
        assert(toWF(BreakpointList(List(breakPoint1), List(breakPoint2))).toWireString === """(:active ((:file """ + file1_str + """ :line 57)) :pending ((:file """ + file1_str + """ :line 59)))""")

        // toWF(config: ProjectConfig)
        //assert(toWF(ProjectConfig()).toWireString === """(:project-name nil :source-roots ())""")
        //assert(toWF(ProjectConfig(name = Some("Project1"), sourceRoots = List(file2))).toWireString === """(:project-name "Project1" :source-roots (""" + file2_str + """))""")

        // toWF(config: ReplConfig)
        assert(toWF(new ReplConfig(Set(file1))).toWireString === s"""(:classpath $file1_str)""")

        // toWF(value: Boolean)
        assert(wfTrue.toWireString === """t""")
        assert(wfFalse.toWireString === """nil""")

        // toWF(value: String)
        assert(toWF("ABC").toWireString === """"ABC"""")

        // toWF(value: Note)
        assert(toWF(note1).toWireString === note1Str)
        //  toWF(notelist: NoteList)
        assert(toWF(noteList).toWireString === noteListStr)

        // toWF(values: Iterable[WireFormat])
        assert(toWF(List(wfTrue, wfFalse, toWF("ABC"))).toWireString === """(t nil "ABC")""")

        // toWF(value: CompletionInfo)
        assert(toWF(completionInfo).toWireString === """(:name "name" :type-sig (((("abc" "def") ("hij" "lmn"))) "ABC") :type-id 88 :to-insert "BAZ")""")

        // toWF(value: CompletionInfo)
        assert(toWF(completionInfo2).toWireString === """(:name "name2" :type-sig (((("abc" "def"))) "ABC") :type-id 90 :is-callable t)""")

        // toWF(value: CompletionInfoList)
        assert(toWF(CompletionInfoList("fooBar", List(completionInfo))).toWireString === """(:prefix "fooBar" :completions ((:name "name" :type-sig (((("abc" "def") ("hij" "lmn"))) "ABC") :type-id 88 :to-insert "BAZ")))""")
        // toWF(value: PackageMemberInfoLight)
        assert(toWF(new PackageMemberInfoLight("packageName")).toWireString === """(:name "packageName")""")
        // toWF(value: SymbolInfo)
        assert(toWF(new SymbolInfo("name", "localName", None, typeInfo, false, Some(2))).toWireString === """(:name "name" :local-name "localName" :type (:name "type1" :type-id 7 :full-name "FOO.type1" :decl-as type1 :outer-type-id 8) :owner-type-id 2)""")

        // toWF(value: NamedTypeMemberInfo)
        assert(toWF(new NamedTypeMemberInfo("typeX", typeInfo, None, 'abcd)).toWireString === """(:name "typeX" :type (:name "type1" :type-id 7 :full-name "FOO.type1" :decl-as type1 :outer-type-id 8) :decl-as abcd)""")

        // toWF(value: EntityInfo)
        assert(toWF(entityInfo).toWireString === entityInfoStr)

        // toWF(value: TypeInfo)
        assert(toWF(typeInfo).toWireString === """(:name "type1" :type-id 7 :full-name "FOO.type1" :decl-as type1 :outer-type-id 8)""")

        // toWF(value: PackageInfo)
        assert(toWF(packageInfo).toWireString === """(:name "name" :info-type package :full-name "fullName")""")

        // toWF(value: CallCompletionInfo)
        assert(toWF(new CallCompletionInfo(typeInfo, List(paramSectionInfo))).toWireString === """(:result-type (:name "type1" :type-id 7 :full-name "FOO.type1" :decl-as type1 :outer-type-id 8) :param-sections ((:params (("ABC" (:name "type1" :type-id 7 :full-name "FOO.type1" :decl-as type1 :outer-type-id 8))))))""")

        // toWF(value: InterfaceInfo)
        assert(toWF(interfaceInfo).toWireString === """(:type (:name "type1" :type-id 7 :full-name "FOO.type1" :decl-as type1 :outer-type-id 8) :via-view "DEF")""")
        // toWF(value: TypeInspectInfo)
        assert(toWF(new TypeInspectInfo(typeInfo, Some(1), List(interfaceInfo))).toWireString === """(:type (:name "type1" :type-id 7 :full-name "FOO.type1" :decl-as type1 :outer-type-id 8) :info-type typeInspect :companion-id 1 :interfaces ((:type (:name "type1" :type-id 7 :full-name "FOO.type1" :decl-as type1 :outer-type-id 8) :via-view "DEF")))""")

        // toWF(value: SymbolSearchResults)
        assert(toWF(new SymbolSearchResults(List(methodSearchRes, typeSearchRes))).toWireString === s"""((:name "abc" :local-name "a" :decl-as abcd :pos (:file $abd_str :line 10) :owner-name "ownerStr") (:name "abc" :local-name "a" :decl-as abcd :pos (:file $abd_str :line 10)))""")

        // toWF(value: ImportSuggestions)
        assert(toWF(new ImportSuggestions(List(List(methodSearchRes, typeSearchRes)))).toWireString ===
          s"""(((:name "abc" :local-name "a" :decl-as abcd :pos (:file $abd_str :line 10) :owner-name "ownerStr") (:name "abc" :local-name "a" :decl-as abcd :pos (:file $abd_str :line 10))))""")

        // toWF(value: SymbolSearchResult)
        assert(toWF(methodSearchRes).toWireString === s"""(:name "abc" :local-name "a" :decl-as abcd :pos (:file $abd_str :line 10) :owner-name "ownerStr")""")
        assert(toWF(typeSearchRes).toWireString === s"""(:name "abc" :local-name "a" :decl-as abcd :pos (:file $abd_str :line 10))""")

        assert(toWF(new ERangePosition(batchSourceFile, 75, 70, 90)).toWireString === """(:file """ + batchSourceFile_str + """ :offset 75 :start 70 :end 90)""")

        assert(toWF(FileRange("/abc", 7, 9)).toWireString === """(:file "/abc" :start 7 :end 9)""")

        // TODO Add all the other symbols
        assert(toWF(SymbolDesignations("/abc", List(
          SymbolDesignation(7, 9, VarFieldSymbol),
          SymbolDesignation(11, 22, ClassSymbol)
        ))).toWireString === """(:file "/abc" :syms ((varField 7 9) (class 11 22)))""")

        assert(toWF(RefactorFailure(7, "message")).toWireString === """(:procedure-id 7 :status failure :reason "message")""")

        assert(toWF(new RefactorEffect(9, 'add, List(TextEdit(file3, 5, 7, "aaa")))).toWireString === """(:procedure-id 9 :refactor-type add :status success :changes ((:file """ + file3_str + """ :text "aaa" :from 5 :to 7)))""")

        assert(toWF(new RefactorResult(7, 'abc, List(file3, file1))).toWireString === """(:procedure-id 7 :refactor-type abc :status success :touched-files (""" + file3_str + " " + file1_str + """))""")

        assert(toWF(Undo(3, "Undoing stuff", List(
          TextEdit(file3, 5, 7, "aaa"),
          NewFile(file4, "xxxxx"),
          DeleteFile(file5, "zzzz")
        ))).toWireString === """(:id 3 :changes ((:file """ + file3_str + """ :text "aaa" :from 5 :to 7) (:file """ + file4_str + """ :text "xxxxx" :from 0 :to 4) (:file """ + file5_str + """ :text "zzzz" :from 0 :to 3)) :summary "Undoing stuff")""")

        assert(toWF(UndoResult(7, List(file3, file4))).toWireString === """(:id 7 :touched-files (""" + file3_str + """ """ + file4_str + """))""")

        assert(wfNull.toWireString === """nil""")
        assert(toWF(DebugVmSuccess).toWireString === """(:status "success")""")
        assert(toWF(DebugVmError(303, "xxxx")).toWireString === """(:status "error" :error-code 303 :details "xxxx")""")
      }
    }
  }
}
