/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.util.gotoByName.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * "Go to | File" action implementation.
 *
 * @author Eugene Belyaev
 * @author Constantine.Plotnikov
 */
public class GotoFileAction extends GotoActionBase implements DumbAware {

  @Override
  public void gotoActionPerformed(AnActionEvent e) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");
    final Project project = e.getData(PlatformDataKeys.PROJECT);
    final GotoFileModel gotoFileModel = new GotoFileModel(project);
    final ChooseByNamePopup popup = ChooseByNamePopup.createPopup(project, gotoFileModel, getPsiContext(e),
                                                                  getInitialText(e.getData(PlatformDataKeys.EDITOR)), FileEditorManagerEx.getInstanceEx(project).hasSplitOrUndockedWindows());
    final ChooseByNameFilter filterUI = new GotoFileFilter(popup, gotoFileModel, project);
    popup.invoke(new ChooseByNamePopupComponent.Callback() {
      public void onClose() {
        if (GotoFileAction.class.equals(myInAction)) {
          myInAction = null;
        }
        filterUI.close();
      }

      public void elementChosen(final Object element) {
        if (element == null) return;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          public void run() {
            Navigatable n = (Navigatable)element;

            //this is for better cursor position
            if (element instanceof PsiFile) {
              VirtualFile vfile = ((PsiFile)element).getVirtualFile();
              if (vfile == null) return;
              n = new OpenFileDescriptor(project, vfile, popup.getLinePosition(), popup.getColumnPosition()).setUseCurrentWindow(popup.isOpenInCurrentWindowRequested());
            }

            if (!n.canNavigate()) return;
            n.navigate(true);
          }
        }, ModalityState.NON_MODAL);
      }
    }, ModalityState.current(), true);
  }

  protected static class GotoFileFilter extends ChooseByNameFilter<FileType> {
    GotoFileFilter(final ChooseByNamePopup popup, GotoFileModel model, final Project project) {
      super(popup, model, GotoFileConfiguration.getInstance(project), project);
    }

    protected List<FileType> getAllFilterValues() {
      List<FileType> elements = new ArrayList<FileType>();
      ContainerUtil.addAll(elements, FileTypeManager.getInstance().getRegisteredFileTypes());
      Collections.sort(elements, FileTypeComparator.INSTANCE);
      return elements;
    }

    protected String textForFilterValue(FileType value) {
      return value.getName();
    }

    protected Icon iconForFilterValue(FileType value) {
      return value.getIcon();
    }
  }

  /**
   * A file type comparator. The comparison rules are applied in the following order.
   * <ol>
   * <li>Unknown file type is greatest.</li>
   * <li>Text files are less then binary ones.</li>
   * <li>File type with greater name is greater (case is ignored).</li>
   * </ol>
   */
  static class FileTypeComparator implements Comparator<FileType> {
    /**
     * an instance of comparator
     */
    static final Comparator<FileType> INSTANCE = new FileTypeComparator();

    /**
     * {@inheritDoc}
     */
    public int compare(final FileType o1, final FileType o2) {
      if (o1 == o2) {
        return 0;
      }
      if (o1 == FileTypes.UNKNOWN) {
        return 1;
      }
      if (o2 == FileTypes.UNKNOWN) {
        return -1;
      }
      if (o1.isBinary() && !o2.isBinary()) {
        return 1;
      }
      if (!o1.isBinary() && o2.isBinary()) {
        return -1;
      }
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  }

}
