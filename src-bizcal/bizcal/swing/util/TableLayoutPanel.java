/*******************************************************************************
 * Bizcal is a component library for calendar widgets written in java using swing.
 * Copyright (C) 2007  Frederik Bertilsson 
 * Contributors:       Martin Heinemann martin.heinemann(at)tudor.lu
 * 
 * http://sourceforge.net/projects/bizcal/
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 * 
 *******************************************************************************/
package bizcal.swing.util;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstraints;

import java.awt.Component;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

public class TableLayoutPanel
    extends JPanel
{
    private static final long serialVersionUID = 1L;
    
    public static final double FILL = TableLayout.FILL;
    public static final double PREFERRED = TableLayout.PREFERRED;
    public static final int RIGHT = TableLayout.RIGHT;
    public static final int LEFT = TableLayout.LEFT;
    public static final int TOP = TableLayout.TOP;
    public static final int BOTTOM = TableLayout.BOTTOM;
    public static final int CENTER = TableLayout.CENTER;
    public static final int FULL = TableLayout.FULL;

    private TableLayout _layout;
    private java.util.List<Column> _columns = new ArrayList<Column>();
    private java.util.List<Row> _rows = new ArrayList<Row>();

    public TableLayoutPanel()
    {
        _layout = new TableLayout();
        setLayout(_layout);
    }
    
    public Column createColumn(double size)
    {
        Column col = new Column(this, size);
        _columns.add(col);
        return col;
    }

    public Column createSpaceColumn(double size)
    {
        Column col = new Column(this, size);
        return col;
    }

    public Column createColumn()
    {
        Column col = new Column(this, TableLayout.PREFERRED);
        _columns.add(col);
        return col;
    }

    public Row createRow(double size)
    {
        Row row = new Row(this, size);
        _rows.add(row);
        return row;
    }

    public Row createRow()
    {
       Row row = new Row(this, TableLayout.PREFERRED);
       _rows.add(row);
       return row;
    }
/*
    public void deleteRow(Row row)
    {
        int rowNo = row.getRowNumber();
        _layout.deleteRow(rowNo);
        _rows.remove(row);
        int noOfRows = _layout.getRow().length;
        for (int i = rowNo; i<noOfRows; i++) {
            ((Row)_rows.get(i)).setRowNumber(i);
        }
    }
*/
    public void deleteRows()
    {
        while(_layout.getRow().length > 0) {
            _layout.deleteRow(_layout.getRow().length - 1);
        }
        _rows.clear();
     }

    public void deleteColumns()
    {
        while(_layout.getColumn().length > 0) {
            _layout.deleteColumn(_layout.getColumn().length - 1);
        }
        _columns.clear();
     }

    public void clear()
    {
        removeAll();
        invalidate();
    }

    public class Row
    {
        private TableLayoutPanel _table;
        private int _rowNo;
        private List cells = new ArrayList();

        private Row(TableLayoutPanel table,
                         double size)
        {
            _table = table;
            _rowNo = table._layout.getNumRow();
            table._layout.insertRow(_rowNo, size);
        }
        
        public Cell createCell(Component component)
        {
            Cell cell = createCell(component, FULL, LEFT);
            cells.add(cell);
            return cell;
        }

        public Cell createCell(Component component, int vAlign, int hAlign)
        {
            Cell cell = new Cell(getNextColumn(), this, component, vAlign, hAlign);
            cells.add(cell);
            return cell;
        }

        public Cell createCell()
        {
            Cell cell = new Cell(getNextColumn(), this);
            cells.add(cell);
            return cell;
        }

        public int getRowNumber()
        {
            return _rowNo;
        }

        public void setRowNumber(int rowNo)
        {
            _rowNo = rowNo;
        }

        private Column getNextColumn()
        {
        	if (cells.isEmpty())
        		return (Column) _columns.get(0);
        	Cell cell = (Cell) cells.get(cells.size()-1);
        	int i = cell.c.col2;
        	return (Column) _columns.get(i+1);        	
        }
    }

    public class Column
    {
        private int _colNo;

        public Column(TableLayoutPanel table,
                           double size)
        {
            _colNo = table._layout.getNumColumn();
            table._layout.insertColumn(_colNo, size);
        }

    }

    public class Cell
    {
        private TableLayoutConstraints c = new TableLayoutConstraints();
        private TableLayoutPanel _table;
        private Component _comp;

        private Cell(Column col,
                     Row row,
                     Component component,
                     int vAlign,
                     int hAlign)
        {
            c.col1 = c.col2 = col._colNo;
            c.row1 = c.row2 = row._rowNo;
            c.vAlign = vAlign;
            c.hAlign = hAlign;
            _table = row._table;
            if (component != null)
                put(component);
        }

        private Cell(Column col,
                     Row row)
        {
            this(col, row, null, FULL, LEFT);
        }

        public void setVerticalAlignment(int align)
        {
            c.vAlign = align;
        }

        public void setHorizontalAlignment(int align)
        {
            c.hAlign = align;
        }

        public void setColumnSpan(int span)
        {
            c.col2 = c.col1 + span -1;
        }

        public void setRowSpan(int span)
        {
            c.row2 = c.row1 + span -1;
        }

        public void put(Component component)
        {
            if (_comp != null) {
                _table.remove(_comp);
                _table.revalidate();
            }
            _comp = component;
            _table.add(component, c);
        }
        

    }

}
