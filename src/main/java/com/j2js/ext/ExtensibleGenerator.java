package com.j2js.ext;

import java.util.List;
import java.util.function.Consumer;

import com.j2js.J2JSCompiler;
import com.j2js.assembly.Project;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableDeclaration;
import com.j2js.visitors.JavaScriptGenerator;

public class ExtensibleGenerator extends JavaScriptGenerator {

	private ExtensionsProvider ext;

	public ExtensibleGenerator(ExtensionsProvider ext, Project theProject, J2JSCompiler compiler) {
		super(theProject, compiler);
		this.ext = ext;
	}

	class ExtEventImpl implements ExtEvent {

		boolean stoped;

		@Override
		public void stopPropagation() {
			stoped = true;
		}
	}

	private <T> void distribute(List<Extension<T>> exts, T node, Consumer<T> finish) {
		ExtEventImpl event = new ExtEventImpl();
		for (Extension<T> e : exts) {
			if (e.canProcess(node)) {
				e.visit(node, getOutputStream(), event);
			}
			if (event.stoped) {
				break;
			}
		}
		if (!event.stoped) {
			finish.accept(node);
		}
	}

	public void visit(TypeDeclaration node) {
		distribute(ext.getClassExtensions(), node, super::visit);
	}

	public void visit(MethodDeclaration node) {
		distribute(ext.getMethodExtensions(), node, super::visit);
	}

	public void visit(VariableDeclaration node) {
		distribute(ext.getFieldExtensions(), node, super::visit);
	}

}
