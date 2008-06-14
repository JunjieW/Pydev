/*
 * Author: atotic
 * Created: July 10, 2003
 * License: Common Public License v1.0
 */

package org.python.pydev.editor;

import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.editor.codecompletion.PythonCompletionProcessor;
import org.python.pydev.editor.codecompletion.PythonStringCompletionProcessor;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;
import org.python.pydev.editor.correctionassist.PythonCorrectionProcessor;
import org.python.pydev.editor.hover.PyAnnotationHover;
import org.python.pydev.editor.hover.PyTextHover;
import org.python.pydev.editor.simpleassist.SimpleAssistProcessor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.ui.ColorCache;

/**
 * Adds simple partitioner, and specific behaviors like double-click actions to the TextWidget.
 * 
 * <p>
 * Implements a simple partitioner that does syntax highlighting.
 * 
 * Changed to a subclass of TextSourceViewerConfiguration as of pydev 1.3.5 
 */
public class PyEditConfiguration extends TextSourceViewerConfiguration {

    private ColorCache colorCache;

    private PyAutoIndentStrategy autoIndentStrategy;

    private String[] indentPrefixes = { "    ", "\t", "" };

    private PyEdit edit;

    private PresentationReconciler reconciler;

    private PyCodeScanner codeScanner;

    private PyColoredScanner commentScanner, stringScanner, backquotesScanner;

    public PyContentAssistant pyContentAssistant = new PyContentAssistant();

    public PyEditConfiguration(ColorCache colorManager, PyEdit edit, IPreferenceStore preferenceStore) {
        super(preferenceStore);
        colorCache = colorManager;
        this.setEdit(edit); 
    }
    
    
    /*
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getHyperlinkDetectorTargets(org.eclipse.jface.text.source.ISourceViewer)
     * @since 3.3
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
        Map targets= super.getHyperlinkDetectorTargets(sourceViewer);
        targets.put("org.python.pydev.editor.PythonEditor", edit); //$NON-NLS-1$
        return targets;
    }

    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new PyAnnotationHover(sourceViewer);
    }
    
    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new PyTextHover(sourceViewer, contentType);
    }

    /**
     * Has to return all the types generated by partition scanner.
     * 
     * The SourceViewer will ignore double-clicks and any other configuration behaviors inside any partition not declared here
     */
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] { IDocument.DEFAULT_CONTENT_TYPE, IPythonPartitions.PY_COMMENT, 
        		IPythonPartitions.PY_SINGLELINE_STRING1, IPythonPartitions.PY_SINGLELINE_STRING2, 
        		IPythonPartitions.PY_MULTILINE_STRING1, IPythonPartitions.PY_MULTILINE_STRING2 };
    }
    
    @Override
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
        return IPythonPartitions.PYTHON_PARTITION_TYPE;
    }

    /**
     * Cache the result, because we'll get asked for it multiple times Now, we always return the PyAutoIndentStrategy. (even on commented lines).
     * 
     * @return PyAutoIndentStrategy which deals with spaces/tabs
     */
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        return new IAutoEditStrategy[] {getPyAutoIndentStrategy()};
    }

    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        if (fPreferenceStore == null || !fPreferenceStore.getBoolean(SpellingService.PREFERENCE_SPELLING_ENABLED))
            return null;

        SpellingService spellingService = EditorsUI.getSpellingService();
        if (spellingService.getActiveSpellingEngineDescriptor(fPreferenceStore) == null)
            return null;
        
        //Overridden (just) to return a PyReconciler!
        IReconcilingStrategy strategy = new PyReconciler(sourceViewer, spellingService); 
        
        MonoReconciler reconciler= new MonoReconciler(strategy, false);
        reconciler.setIsIncrementalReconciler(false);
        reconciler.setProgressMonitor(new NullProgressMonitor());
        reconciler.setDelay(500);
        return reconciler;
    }
    
    /**
     * Cache the result, because we'll get asked for it multiple times Now, we always return the PyAutoIndentStrategy. (even on commented lines).
     * 
     * @return PyAutoIndentStrategy which deals with spaces/tabs
     */
    public PyAutoIndentStrategy getPyAutoIndentStrategy() {
        if (autoIndentStrategy == null) {
            autoIndentStrategy = new PyAutoIndentStrategy();
        }
        return autoIndentStrategy;
    }
    
    /**
     * Recalculates indent prefixes based upon preferences
     * 
     * we hold onto the same array SourceViewer has, and write directly into it. This is because there is no way to tell SourceViewer that indent prefixes have changed. And we need this functionality
     * when user resets the tabs vs. spaces preference
     */
    public void resetIndentPrefixes() {
        Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
        int tabWidth = DefaultIndentPrefs.getStaticTabWidth();
        FastStringBuffer spaces = new FastStringBuffer(8);

        for (int i = 0; i < tabWidth; i++) {
            spaces.append(" ");
        }

        boolean spacesFirst = prefs.getBoolean(PydevPrefs.SUBSTITUTE_TABS) && !(getPyAutoIndentStrategy()).getIndentPrefs().getForceTabs();

        if (spacesFirst) {
            indentPrefixes[0] = spaces.toString();
            indentPrefixes[1] = "\t";
        } else {
            indentPrefixes[0] = "\t";
            indentPrefixes[1] = spaces.toString();
        }
    }

    /**
     * Prefixes used in shift-left/shift-right editor operations
     * 
     * shift-right uses prefix[0] shift-left removes a single instance of the first prefix from the array that matches
     * 
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getIndentPrefixes(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
     */
    public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
        resetIndentPrefixes();
        sourceViewer.setIndentPrefixes(indentPrefixes, contentType);
        return indentPrefixes;
    }

    /**
     * Just the default double-click strategy for now. But we should be smarter.
     * 
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getDoubleClickStrategy(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
     */
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
        if (contentType.equals(IDocument.DEFAULT_CONTENT_TYPE))
            return new PyDoubleClickStrategy();
        else
            return super.getDoubleClickStrategy(sourceViewer, contentType);
    }

    /**
     * TabWidth is defined inside pydev preferences.
     * 
     * Python uses its own tab width, since I think that its standard is 8
     */
    public int getTabWidth(ISourceViewer sourceViewer) {
        return DefaultIndentPrefs.getStaticTabWidth();
    }

    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

        if (reconciler == null) {
            reconciler = new PresentationReconciler();
            reconciler.setDocumentPartitioning(IPythonPartitions.PYTHON_PARTITION_TYPE);

            DefaultDamagerRepairer dr;

            // DefaultDamagerRepairer implements both IPresentationDamager, IPresentationRepairer
            // IPresentationDamager::getDamageRegion does not scan, just
            // returns the intersection of document event, and partition region
            // IPresentationRepairer::createPresentation scans
            // gets each token, and sets text attributes according to token

            // We need to cover all the content types from PyPartitionScanner

            IPreferenceStore preferences = PydevPlugin.getChainedPrefStore();
            // Comments have uniform color
            commentScanner = new PyColoredScanner(colorCache, PydevPrefs.COMMENT_COLOR, preferences.getInt(PydevPrefs.COMMENT_STYLE));
            dr = new DefaultDamagerRepairer(commentScanner);
            reconciler.setDamager(dr, IPythonPartitions.PY_COMMENT);
            reconciler.setRepairer(dr, IPythonPartitions.PY_COMMENT);

            // Backquotes have uniform color
            backquotesScanner = new PyColoredScanner(colorCache, PydevPrefs.BACKQUOTES_COLOR,preferences.getInt(PydevPrefs.BACKQUOTES_STYLE));
            dr = new DefaultDamagerRepairer(backquotesScanner);
            reconciler.setDamager(dr, IPythonPartitions.PY_BACKQUOTES);
            reconciler.setRepairer(dr, IPythonPartitions.PY_BACKQUOTES);
            
            // Strings have uniform color
            stringScanner = new PyColoredScanner(colorCache, PydevPrefs.STRING_COLOR,preferences.getInt(PydevPrefs.STRING_STYLE));
            dr = new DefaultDamagerRepairer(stringScanner);
            reconciler.setDamager(dr, IPythonPartitions.PY_SINGLELINE_STRING1);
            reconciler.setRepairer(dr, IPythonPartitions.PY_SINGLELINE_STRING1);
            reconciler.setDamager(dr, IPythonPartitions.PY_SINGLELINE_STRING2);
            reconciler.setRepairer(dr, IPythonPartitions.PY_SINGLELINE_STRING2);
            reconciler.setDamager(dr, IPythonPartitions.PY_MULTILINE_STRING1);
            reconciler.setRepairer(dr, IPythonPartitions.PY_MULTILINE_STRING1);
            reconciler.setDamager(dr, IPythonPartitions.PY_MULTILINE_STRING2);
            reconciler.setRepairer(dr, IPythonPartitions.PY_MULTILINE_STRING2);

            // Default content is code, we need syntax highlighting
            codeScanner = new PyCodeScanner(colorCache);
            dr = new DefaultDamagerRepairer(codeScanner);
            reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
            reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
        }

        return reconciler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(org.eclipse.jface.text.source.ISourceViewer)
     */
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		// next create a content assistant processor to populate the completions window
        IContentAssistProcessor processor = new SimpleAssistProcessor(edit, 
        		new PythonCompletionProcessor(edit, pyContentAssistant), pyContentAssistant);
        
        PythonStringCompletionProcessor stringProcessor = new PythonStringCompletionProcessor(edit, pyContentAssistant);

        pyContentAssistant.setRestoreCompletionProposalSize(getSettings("pydev_completion_proposal_size")); 

        // No code completion in comments
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_SINGLELINE_STRING1);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_SINGLELINE_STRING2);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_MULTILINE_STRING1);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_MULTILINE_STRING2);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_COMMENT);
        pyContentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
        pyContentAssistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        pyContentAssistant.enableAutoActivation(true); //always true, but the chars depend on whether it is activated or not in the preferences

        //note: delay and auto activate are set on PyContentAssistant constructor.

        pyContentAssistant.setDocumentPartitioning(IPythonPartitions.PYTHON_PARTITION_TYPE);
        pyContentAssistant.setAutoActivationDelay(PyCodeCompletionPreferencesPage.getAutocompleteDelay());

        
        return pyContentAssistant;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getQuickAssistAssistant(org.eclipse.jface.text.source.ISourceViewer)
     */
    public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
        // create a content assistant:
        PyCorrectionAssistant assistant = new PyCorrectionAssistant();

        // next create a content assistant processor to populate the completions window
        IQuickAssistProcessor processor = new PythonCorrectionProcessor(this.getEdit());

        // Correction assist works on all
        assistant.setQuickAssistProcessor(processor);
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

        //delay and auto activate set on PyContentAssistant constructor.
        
        return assistant;
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getInformationControlCreator(org.eclipse.jface.text.source.ISourceViewer)
     */
    public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
        return PyContentAssistant.createInformationControlCreator(sourceViewer);
    }

    /**
     * Returns the settings for the given section.
     *
     * @param sectionName the section name
     * @return the settings
     * @since pydev 1.3.5
     */
    private IDialogSettings getSettings(String sectionName) {
        IDialogSettings settings= PydevPlugin.getDefault().getDialogSettings().getSection(sectionName);
        if (settings == null)
            settings= PydevPlugin.getDefault().getDialogSettings().addNewSection(sectionName);

        return settings;
    }


    /**
     * @param edit The edit to set.
     */
    private void setEdit(PyEdit edit) {
        this.edit = edit;
    }

    /**
     * @return Returns the edit.
     */
    private PyEdit getEdit() {
        return edit;
    }

    //updates the syntax highlighting for the specified preference
    //assumes that that editor colorCache has been updated with the
    //new named color
    public void updateSyntaxColorAndStyle() {
        if (reconciler != null) {
            //always update all (too much work in keeping this synchronized by type)
            codeScanner.updateColors();
            
            IPreferenceStore preferences = PydevPlugin.getChainedPrefStore();
            
            commentScanner.setStyle(preferences.getInt(PydevPrefs.COMMENT_STYLE));
            commentScanner.updateColorAndStyle();

            stringScanner.setStyle(preferences.getInt(PydevPrefs.STRING_STYLE));
            stringScanner.updateColorAndStyle();

            backquotesScanner.setStyle(preferences.getInt(PydevPrefs.BACKQUOTES_STYLE));
            backquotesScanner.updateColorAndStyle();
        }
    }
    
}