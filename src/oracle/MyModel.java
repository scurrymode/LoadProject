/*
 * JTable 이 수시로 정보를 받아갈 것~!
 * */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	Vector <String>columnName; //컬럼의 제목을 담을 벡터
	Vector<Vector> list; //레코드를 담을 이차원 벡터
	
	public MyModel(Vector list, Vector columnName) {
		this.list=list;
		this.columnName=columnName;
	}
	

	//row, col에 위치한 셀을 편집 가능하게 한다.
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	
	//각셀의 변경값을 반영하는 메서드 오버라이드
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
