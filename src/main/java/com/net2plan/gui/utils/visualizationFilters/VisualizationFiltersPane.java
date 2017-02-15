package com.net2plan.gui.utils.visualizationFilters;

import com.net2plan.gui.utils.*;
import com.net2plan.gui.utils.offlineExecPane.OfflineExecutionPanel;
import com.net2plan.gui.utils.visualizationFilters.VisualizationFiltersController;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author César
 * @date 19/11/2016
 * Class TableButton seen in
 * http://stackoverflow.com/questions/1475543/how-to-add-button-in-a-row-of-jtable-in-swing-java/1475625#1475625?newreg=e3d8c64a54b44aa6980ff4b0c6a5a123
 */
public class VisualizationFiltersPane extends JPanel
{
    private final IVisualizationCallback mainWindow;
    private JButton load, deleteAll, activeAll, deactiveAll;
    private JRadioButton andButton, orButton;
    private static JFileChooser fileChooser;
    private File selectedFile;
    private final AdvancedJTable filtersTable;
    private AdvancedJTable parametersTable;
    private final static TableCellRenderer CHECKBOX_RENDERER;
    private final static Object[] HEADER, HEADER_PARAM;
    private final JTextArea descriptionArea;
    private final JTextField txt_file;
    private final JLabel filtersLabel;
    private final Class [] columnClass;
    private Map<String, Class> implementations;
    private VisualizationFiltersController filtersController;
    private List<Triple<String, String, String>> currentParameters;
    private String currentFilterName;

    static
    {
        CHECKBOX_RENDERER = new CheckBoxRenderer();
        HEADER = StringUtils.arrayOf("Remove","Filter","Active");
        HEADER_PARAM = StringUtils.arrayOf("Parameter","Value","Description");
    }

    public VisualizationFiltersPane(IVisualizationCallback mainWindow)
    {
        super();

        this.mainWindow = mainWindow;

        filtersController = VisualizationFiltersController.getController();
        Object[][] data = {{null, null, null}};
        File FILTERS_DIRECTORY = new File(IGUIModule.CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        FILTERS_DIRECTORY = FILTERS_DIRECTORY.isDirectory() ? FILTERS_DIRECTORY : IGUIModule.CURRENT_DIR;

        fileChooser = new JFileChooser(FILTERS_DIRECTORY);
        TableModel filtersModel = new ClassAwareTableModelImpl(data, HEADER);
        TableModel parametersModel = new ClassAwareTableModelImpl2(data, HEADER_PARAM);

        filtersTable = new AdvancedJTable(filtersModel);
        parametersTable = new AdvancedJTable(parametersModel);
        TableColumn removeColumn = filtersTable.getColumn("Remove");
        TableColumn activeColumn = filtersTable.getColumn("Active");
        removeColumn.setResizable(false);
        removeColumn.setMinWidth(90);
        removeColumn.setMaxWidth(90);
        activeColumn.setResizable(false);
        activeColumn.setMinWidth(60);
        activeColumn.setMaxWidth(60);
        updateFiltersTable();

        filtersLabel = new JLabel("Parameters");
        descriptionArea = new JTextArea();
        descriptionArea.setFont(new JLabel().getFont());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setColumns(80);
        descriptionArea.setRows(15);

        txt_file = new JTextField();
        txt_file.setEditable(false);

        columnClass = new Class[3];
        columnClass[0] = TableButton.class;
        columnClass[1] = String.class;
        columnClass[2] = Boolean.class;


        setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow]"));


        load = new JButton("Load");
        load.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    int rc = fileChooser.showOpenDialog(null);
                    if (rc != JFileChooser.APPROVE_OPTION) return;
                    selectedFile = fileChooser.getSelectedFile();
                    loadImplementations(selectedFile);
                } catch (NoRunnableCodeFound ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to load");
                } catch (Throwable ex)
                {
                    ErrorHandling.addErrorOrException(ex, RunnableSelector.class);
                    ErrorHandling.showErrorDialog("Error loading runnable code");
                }
            }
        });

        deleteAll = new JButton("Remove All");
        deleteAll.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                filtersController.removeAllVisualizationFilters();
                descriptionArea.setText("");
                txt_file.setText("");
                filtersLabel.setText("Parameters");
                ((DefaultTableModel) parametersTable.getModel()).setDataVector(new Object[0][3],HEADER_PARAM);
                updateFiltersTable();
                mainWindow.updateVisualizationAfterNewTopology();
            }
        });
        andButton = new JRadioButton("AND filtering mode (Shows the Network Element if is visible in all filters)");
        andButton.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (andButton.isSelected())
                {
                    orButton.setSelected(false);
                    filtersController.updateFilteringMode("AND");
                    mainWindow.updateVisualizationAfterNewTopology();
                }
            }
        });
        orButton = new JRadioButton("OR filtering mode (Shows the Network Element if is visible in, at least, one of the filters)");
        orButton.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (orButton.isSelected())
                {
                    andButton.setSelected(false);
                    filtersController.updateFilteringMode("OR");
                    mainWindow.updateVisualizationAfterNewTopology();
                }
            }
        });
        activeAll = new JButton("Active All");
        activeAll.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                for(IVisualizationFilter vf : filtersController.getCurrentVisualizationFilters())
                {
                    filtersController.activateVisualizationFilter(vf);
                }
                updateFiltersTable();
                mainWindow.updateVisualizationAfterNewTopology();
            }
        });
        deactiveAll = new JButton("Deactive All");
        deactiveAll.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                for(IVisualizationFilter vf : filtersController.getCurrentVisualizationFilters())
                {
                    filtersController.deactivateVisualizationFilter(vf);
                }
                updateFiltersTable();
                mainWindow.updateVisualizationAfterNewTopology();
            }
        });
        andButton.setSelected(true);
        setLayout(new MigLayout("", "[][grow][]", "[][][][][grow]"));
        add(new JLabel("Filters File"),"spanx");
        add(txt_file, "grow, spanx");
        add(load, "spanx ");
        add(new JLabel("Filtering Options"), "top, growx, spanx 2, wrap, wmin 100");
        add(andButton,"wrap");
        add(orButton, "wrap");
        add(new JLabel("Filters"), "spanx 3, wrap");
        add(deleteAll);
        add(activeAll);
        add(deactiveAll, "spanx 3, wrap");
        add(new JScrollPane(filtersTable), "spanx 3, grow, wrap");
        add(new JLabel("Description"), "spanx 3, wrap");
        add(new JScrollPane(descriptionArea),"spanx 3, grow, wrap");
        add(filtersLabel, "spanx 3, wrap");
        add(new JScrollPane(parametersTable),"spanx 3, grow, wrap");
        filtersTable.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int clickedRow = filtersTable.rowAtPoint(e.getPoint());
                int clickedColumn = filtersTable.columnAtPoint(e.getPoint());
                String selectedFilter = (String)filtersTable.getModel().getValueAt(clickedRow,1);

                if(clickedColumn == 0)
                {
                    filtersController.removeVisualizationFilter(selectedFilter);
                    if(filtersController.getCurrentVisualizationFilters().size() == 0)
                        txt_file.setText("");
                    descriptionArea.setText("");
                    updateFiltersTable();
                    filtersLabel.setText("Parameters");
                    ((DefaultTableModel) parametersTable.getModel()).setDataVector(new Object[0][2],HEADER_PARAM);
                    mainWindow.updateVisualizationAfterNewTopology();
                }
                else{

                    IVisualizationFilter vf = filtersController.getVisualizationFilterByName(selectedFilter);
                    currentFilterName = selectedFilter;
                    descriptionArea.setText(vf.getDescription());
                    updateParametersTable(selectedFilter);

                }

            }

            @Override
            public void mousePressed(MouseEvent e)
            {

            }

            @Override
            public void mouseReleased(MouseEvent e)
            {

            }

            @Override
            public void mouseEntered(MouseEvent e)
            {

            }

            @Override
            public void mouseExited(MouseEvent e)
            {

            }
        });

    }

    private void loadImplementations(File f) {
        try {
            if (!f.isAbsolute()) f = new File(SystemUtils.getCurrentDir(), f.getPath());

            Map<String, Class> aux_implementations = new TreeMap<String, Class>();
            List<Class<IVisualizationFilter>> aux = ClassLoaderUtils.getClassesFromFile(f, IVisualizationFilter.class);
            for (Class<IVisualizationFilter> implementation : aux) {

                if (IVisualizationFilter.class.isAssignableFrom(implementation)) {

                    aux_implementations.put(implementation.getName(), IVisualizationFilter.class);
                }
            }


            if (aux_implementations.isEmpty())
                throw new NoRunnableCodeFound(f, new LinkedHashSet<Class<? extends IExternal>>() {
                    {
                        add(IVisualizationFilter.class);
                    }
                });

            implementations = aux_implementations;

            txt_file.setText(f.getCanonicalPath());

            for(Map.Entry<String,Class> implValue : implementations.entrySet())
            {
                IVisualizationFilter instance = ClassLoaderUtils.getInstance(f,implValue.getKey(),IVisualizationFilter.class);
                filtersController.addVisualizationFilter(instance);
                filtersController.deactivateVisualizationFilter(instance);
                if(instance.getUniqueName() == null)
                    throw new Net2PlanException("Visualization Filters must have a name");
                ((Closeable) instance.getClass().getClassLoader()).close();
            }
            updateFiltersTable();
            mainWindow.updateVisualizationAfterNewTopology();

        } catch (NoRunnableCodeFound e) {
            throw (e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateFiltersTable(){

        TableModel tm = filtersTable.getModel();
        ArrayList<IVisualizationFilter> currentVisFilters = filtersController.getCurrentVisualizationFilters();
        int length = currentVisFilters.size();
        Object[][] newData = new Object[length][HEADER.length];
        IVisualizationFilter vf;
        for(int i = 0;i<length;i++)
        {
            vf = currentVisFilters.get(i);
            newData[i][0] = new TableButton("Remove");
            newData[i][1] = vf.getUniqueName();
            newData[i][2] = filtersController.isVisualizationFilterActive(vf);
        }


        ((DefaultTableModel) tm).setDataVector(newData,HEADER);
        for(int j = 0;j<length;j++){
            filtersTable.setCellRenderer(j,0,new TableButton("Remove"));
            filtersTable.setCellRenderer(j,2,CHECKBOX_RENDERER);
        }

        TableColumn removeColumn = filtersTable.getColumn("Remove");
        TableColumn activeColumn = filtersTable.getColumn("Active");
        removeColumn.setResizable(false);
        removeColumn.setMinWidth(90);
        removeColumn.setMaxWidth(90);
        activeColumn.setResizable(false);
        activeColumn.setMinWidth(60);
        activeColumn.setMaxWidth(60);


    }

    public void updateParametersTable(String vfName)
    {
        filtersLabel.setText(vfName+" Parameters");
        setParameters(vfName);

    }

    public void setParameters(String vfName) {

        Map<String, String> parameters = filtersController.getFilterParameters(vfName);
        List<Triple<String, String, String>> defaultParameters = filtersController.getDefaultParameters(vfName);
        Object[][] data = new Object[parameters.size()][HEADER_PARAM.length];
        int counter = 0;
        parametersTable.removeAll();
        for(Triple<String, String, String> t : defaultParameters) {

            String defaultValue = t.getSecond().toLowerCase(Locale.getDefault());
            if (defaultValue.startsWith("#select#")) {
                String auxOptions = defaultValue.replaceFirst("#select#", "").trim();
                String[] options = StringUtils.split(auxOptions, ", ");
                if (options.length > 0) {
                    data[counter][0] = t.getFirst();
                    data[counter][1] = parameters.get(t.getFirst());
                    data[counter][2] = t.getThird();
                    addComboCellEditor(options, counter, 1);
                    counter++;
                    continue;
                }
            } else if (defaultValue.startsWith("#boolean#")) {
                String flag = parameters.get(t.getFirst()).replaceFirst("#select#", "").trim();
                boolean isSelected = Boolean.parseBoolean(flag);
                data[counter][0] = t.getFirst();
                data[counter][1] = Boolean.toString(isSelected);
                data[counter][2] = t.getThird();
                addCheckboxCellEditor(isSelected, counter, 1);
                counter++;
                continue;
            }
            else{
                data[counter][0] = t.getFirst();
                data[counter][1] = parameters.get(t.getFirst());
                data[counter][2] = t.getThird();
                addTextCellEditor(counter,1);
                counter++;
                continue;
            }

        }
        ((DefaultTableModel)parametersTable.getModel()).setDataVector(data, HEADER_PARAM);

    }

    private void addCheckboxCellEditor(boolean defaultValue, int rowIndex, int columnIndex) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setHorizontalAlignment(JLabel.CENTER);
        checkBox.setSelected(defaultValue);
        parametersTable.setCellEditor(rowIndex, columnIndex, new DefaultCellEditor(checkBox));
        parametersTable.setCellRenderer(rowIndex, columnIndex, CHECKBOX_RENDERER);
    }

    private void addComboCellEditor(String[] options, int rowIndex, int columnIndex) {
        JComboBox comboBox = new JComboBox();
        for (String option : options) comboBox.addItem(option);
        parametersTable.setCellEditor(rowIndex, columnIndex, new DefaultCellEditor(comboBox));
        parametersTable.setCellRenderer(rowIndex, columnIndex, new DefaultTableCellRenderer());
    }

    private void addTextCellEditor(int rowIndex, int columnIndex){
        JTextField textField = new JTextField();
        parametersTable.setCellEditor(rowIndex, columnIndex, new DefaultCellEditor(textField));
        parametersTable.setCellRenderer(rowIndex, columnIndex, new DefaultTableCellRenderer());
    }

    public Map<String, String> getParameters() {
        Map<String, String> out = new LinkedHashMap<String, String>();

        TableModel model = parametersTable.getModel();

        int numRows = model.getRowCount();
        for (int rowId = 0; rowId < numRows; rowId++)
            out.put(model.getValueAt(rowId, 0).toString(), model.getValueAt(rowId, 1).toString());


        return out;
    }




    private static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer
    {
        public CheckBoxRenderer()
        {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (isSelected)
            {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else
            {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            setSelected(value != null && Boolean.parseBoolean(value.toString()));
            return this;
        }
    }

    private class ClassAwareTableModelImpl extends ClassAwareTableModel
    {
        public ClassAwareTableModelImpl(Object[][] dataVector, Object[] columnIdentifiers)
        {
            super(dataVector, columnIdentifiers);
        }

        @Override
        public Class getColumnClass(int col)
        {
            return columnClass[col];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if(filtersController.getCurrentVisualizationFilters().size() == 0) return false;
            if(columnIndex == 2 || columnIndex == 0) return true;
            return false;
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            if(column == 2)
            {
                if(value == null) return;
                boolean active = (Boolean) value;
                String filterToChange = (String)filtersTable.getModel().getValueAt(row,1);
                IVisualizationFilter vf = filtersController.getVisualizationFilterByName(filterToChange);
                if(active)
                {
                    filtersController.activateVisualizationFilter(vf);
                }
                else{
                    filtersController.deactivateVisualizationFilter(vf);
                }
                super.setValueAt(value, row, column);
                updateFiltersTable();
                mainWindow.updateVisualizationAfterNewTopology();
            }
        }
    }




    private static class SortByParameterNameComparator implements Comparator<Triple<String, String, String>>
    {
        @Override
        public int compare(Triple<String, String, String> o1, Triple<String, String, String> o2)
        {
            return o1.getFirst().compareTo(o2.getFirst());
        }
    }

    private class ClassAwareTableModelImpl2 extends ClassAwareTableModel
    {
        public ClassAwareTableModelImpl2(Object[][] dataVector, Object[] columnIdentifiers)
        {
            super(dataVector, columnIdentifiers);
        }

        @Override
        public Class getColumnClass(int col)
        {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if(filtersController.getCurrentVisualizationFilters().size() == 0) return false;

            if (columnIndex == 1) {
                int rowView = parametersTable.convertRowIndexToModel(rowIndex);
                int columnView = parametersTable.convertColumnIndexToView(columnIndex);
                TableCellEditor tce = parametersTable.getCellEditor(rowView, columnView);

                if (tce instanceof ActionTableCellEditor) {
                    return true;
                } else if (tce instanceof DefaultCellEditor) {
                    Component cellComponent = ((DefaultCellEditor) tce).getComponent();
                    if (cellComponent instanceof JComboBox || cellComponent instanceof JCheckBox) {
                        return true;
                    } else {
                        Constants.RunnableCodeType runnableCodeType = Constants.RunnableCodeType.find(getValueAt(rowIndex, 1).toString());
                        if (runnableCodeType == null) return true;
                    }
                }
            }

            return false;

        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            if(value == null)return;
            if(value instanceof Boolean)
            {
                boolean flag = (Boolean)value;
                String newParamValue = "";
                if(flag)
                    newParamValue = "true";
                else{
                    newParamValue = "false";
                }
                String parameterName = (String) getValueAt(row,0);
                filtersController.updateParameter(currentFilterName,parameterName,newParamValue);
            }
            else{
                String newValue = (String)value;
                String parameterName = (String) getValueAt(row,0);
                filtersController.updateParameter(currentFilterName,parameterName,newValue);
            }


            super.setValueAt(value,row,column);
            mainWindow.updateVisualizationAfterNewTopology();
        }
    }

    private static class TableButton extends JButton implements TableCellRenderer, TableCellEditor {
        private int selectedRow;
        private int selectedColumn;

        public TableButton(String text) {
            super(text);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            return this;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value, boolean isSelected, int row, int col) {
            selectedRow = row;
            selectedColumn = col;
            return this;
        }

        @Override
        public void addCellEditorListener(CellEditorListener arg0) {
        }

        @Override
        public void cancelCellEditing() {
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public boolean isCellEditable(EventObject arg0) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener arg0) {
        }

        @Override
        public boolean shouldSelectCell(EventObject arg0) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            return true;
        }
    }
}




