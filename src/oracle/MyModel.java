/*
 * JTable �� ���÷� ������ �޾ư� ��~!
 * */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	Vector <String>columnName; //�÷��� ������ ���� ����
	Vector<Vector> list; //���ڵ带 ���� ������ ����
	
	public MyModel(Vector list, Vector columnName) {
		this.list=list;
		this.columnName=columnName;
	}
	

	//row, col�� ��ġ�� ���� ���� �����ϰ� �Ѵ�.
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	
	//������ ���氪�� �ݿ��ϴ� �޼��� �������̵�
	public void setValueAt(Object value, int row, int col) {
		Vector vec = list.get(row);
		vec.set(col, value);
		fireTableCellUpdated(row, col);
	}
		
	@Override
	public String getColumnName(int col) {
		return columnName.elementAt(col);
	}
	
	@Override
	public int getColumnCount() {
		return columnName.size();
		
	}

	@Override
	public int getRowCount() {
		
		return list.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		Vector vec=list.get(row);
		
		return vec.get(col);
	}

}
