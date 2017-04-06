package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

import util.file.FileUtil;



public class LoadMain extends JFrame implements ActionListener, TableModelListener, Runnable{
	JPanel p_north;
	JTextField t_path;
	JButton bt_open, bt_load, bt_excel, bt_del;
	JTable table;
	JScrollPane scroll;
	JFileChooser chooser;
	FileReader reader;
	BufferedReader buffr;
	
	//������â�� ������ �̸� ������ Ȯ���س���!
	DBManager manager;
	Connection con;
	
	Vector list;
	Vector columnName;
	
	Thread thread; //���� ��Ͻ� ���� ������ ���� ����? �����;��� �ʹ� ���ų�, ��Ʈ��ũ ���°� �������� ���, insert�� while�� �ӵ��� ������ �� �ֱ⿡ �������� ���ؼ�
	//�Ϻη� �ð� ������ ����Ų��.
	
	//���� ���Ͽ� ���� ������ �������� �����尡 ����� �� �ִ� ���·� �����س���!
	StringBuffer sql = new StringBuffer();

	public LoadMain() {
		p_north = new JPanel();
		t_path = new JTextField(20);
		bt_open = new JButton("csv���Ͽ���");
		bt_load = new JButton("�ε��ϱ�");
		bt_excel = new JButton("excel�ε�");
		bt_del = new JButton("�����ϱ�");
		
		table = new JTable();
		scroll = new JScrollPane(table);
		
		chooser = new JFileChooser("c:/animal/");
		
		p_north.add(t_path);
		p_north.add(bt_open);
		p_north.add(bt_load);
		p_north.add(bt_excel);
		p_north.add(bt_del);
		
		add(p_north, BorderLayout.NORTH);
		add(scroll);
		
		//��ư�� ������ ���̱�!
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_excel.addActionListener(this);
		bt_del.addActionListener(this);		
		
		//������� ������ ����
		//�����츮���ʴ� �������̵��� �� ����, ���������� extends�Ϸ��ߴ��� �̹� JFrame�ϰ� �־ �����͸�����
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//�����ͺ��̽� �ڿ�����
				manager.disConnect(con);
				//���μ��� ����
				System.exit(0);
			}
		});
		
		
		
		setVisible(true);
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		init();
	}
	
	public void init(){
		manager=DBManager.getInstance();
		con=manager.getConnection();
	}
	
	//���� Ž���� ����
	public void open(){
		int result = chooser.showOpenDialog(this);
		
		//���⸦ ������.. ���������� ��Ʈ���� ��������!
		if(result==JFileChooser.APPROVE_OPTION){
			
						
			//������ ������ ����
			File file = chooser.getSelectedFile();
			
			//csv���� �ƴϸ� ����!
			String name = file.getName();
			
			String ext=FileUtil.getExt(name);
			
			if(!ext.equals("csv")){
				JOptionPane.showMessageDialog(this, "csv ���ϸ� �������ּ���");
				return;
			}
			
				t_path.setText(file.getAbsolutePath());
				
				try {
					//��Ʈ���� ����� ����!
					reader = new FileReader(file);
					buffr = new BufferedReader(reader);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} 		
				
			
		}
	}
	
	//CSV->Oracle�� ������ ����(migration)�ϱ�
	public void load(){
		//���۽�Ʈ���� �̿��Ͽ� csv�� �����͸� ���پ� �о�鿩 insert��Ű��! ���ڵ尡 ���������� �׷��� while������ ������ �ʹ� �����Ƿ� Thread�� sleep����!
		String data;
		StringBuffer sb = new StringBuffer();
		PreparedStatement pstmt=null;
		
		try {
			while(true){
				data=buffr.readLine();
				
				if(data==null) break;
				
				String[] value=data.split(",");
				
				//seq���� �����ϰ� insert�ϰڴ�.
				if(!value[0].equals("seq")){
					sb.append("insert into hospital(seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values ("+value[0]+", '"+value[1]+"', '"+value[2]+"', '"+value[3]+"', '"+value[4]+"', "+value[5]+", '"+value[6]+"')");
					
					pstmt=con.prepareStatement(sb.toString());
					int result=pstmt.executeUpdate(); //��������
					//������ ������ StringBuffer �����͸� ��� �����
					sb.delete(0, sb.length());
					
				}else{
					System.out.println("�� 1���̹Ƿ� ����");
				}
			}
			JOptionPane.showMessageDialog(this, "Migration�Ϸ�!!");
			
			//JTable ������ ó��!!
			getList();
			//���̺� �𵨷� ���̺��� �����!
			table.setModel(new MyModel(list, columnName));
			
			//���̺� �𵨰� �����ʿ��� ����
			table.getModel().addTableModelListener(this);
			
			table.updateUI();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	//�������� �о db�� ���̱׷��̼� �ϱ�!!
	//javaSE �������� ���̺귯���� ����!
	//open Source ��������Ʈ����
	//copyright <---> copyleft(����ġ ��ü)
	//POI ���̺귯��! http://apache.org
	/*
	 * HSSFWorkbook
	 * HSSFSheet
	 * HSSFRow
	 * HSSFCell
	 * */
	
	public void loadExcel(){
		FileInputStream fis=null;
		StringBuffer cols=new StringBuffer();
		StringBuffer data=new StringBuffer();
		
		//POI HSSFWorkbook �����ϱ� ���ؼ� ���븦 �žҴ�.
		int option=chooser.showOpenDialog(this);
		if(option==JFileChooser.APPROVE_OPTION){
			File file =chooser.getSelectedFile();
			try {
				fis=new FileInputStream(file);
				
				HSSFWorkbook book=null;
				book=new HSSFWorkbook(fis);
				HSSFSheet sheet=null;
				sheet=book.getSheet("sheet1");
				DataFormatter df=new DataFormatter();
				String[] values=null;
								
				int total = sheet.getLastRowNum();
				
				/*-------------------------------------------
				 ù��° row�� �����Ͱ� �ƴ� �÷� �����̹Ƿ�,
				 �� �������� �����Ͽ� insert into table(***)�� ����
				 -------------------------------------------*/
				
				HSSFRow firstRow=sheet.getRow(sheet.getFirstRowNum());
				//row�� ������� �÷��� �м��Ѵ�
				//int last = firstRow.getLastCellNum();//������ cell ��ȣ
				for(int i=0; i<firstRow.getLastCellNum();i++){
					HSSFCell nameCell=firstRow.getCell(i);
					if(i<firstRow.getLastCellNum()-1){
						cols.append(nameCell.getStringCellValue()+",");
						//System.out.print(nameCell.getStringCellValue()+",");
					}else{
						cols.append(nameCell.getStringCellValue());
						//System.out.print(nameCell.getStringCellValue());
					}
				}				
				
				//������ ����
				for(int a=1; a<=total;a++){
					HSSFRow row=sheet.getRow(a);
					int col=row.getLastCellNum();
					values = new String[col];
					
					data.delete(0, data.length());
					
					for(int i=0;i<col;i++){
						HSSFCell cell=row.getCell(i);
						String value=df.formatCellValue(cell);
						
						if(cell.getCellType()==HSSFCell.CELL_TYPE_STRING){
							value="'"+value+"'";
						}
						
						if(i<col-1){
							data.append(value+",");
						}else{
							data.append(value);
						}
					}
					sql.append("insert into hospital("+cols.toString()+")");
					sql.append(" values ("+data.toString()+");");
					
				}
				//���� ��������, ����ϰ� �����忡�� �� ��Ű��!
				thread = new Thread(this);//this�� �������ν�, Runnable�� Ŭ������ run�� �����Ѵ�.
				thread.start();
				
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if(fis!=null){
						try {
							fis.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}					
				}	
		}
	}
	
	//��� ���ڵ� ��������!!
	public void getList(){
		String sql = "select * from hospital order by seq asc";
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			ResultSetMetaData meta = rs.getMetaData();
			
			//�÷��̸��� ���Ϳ� ����~!
			int count = meta.getColumnCount();
			
			columnName=new Vector();
			
			for(int i=0;i<count;i++){
				columnName.add(meta.getColumnName(i+1));
			}
			
			list = new Vector<Vector>(); //������ ����
			
			while(rs.next()){
				Vector vec = new Vector(); //���ڵ� 1�� ���� ����
				vec.add(rs.getString("seq"));
				vec.add(rs.getString("name"));
				vec.add(rs.getString("addr"));
				vec.add(rs.getString("regdate"));
				vec.add(rs.getString("status"));
				vec.add(rs.getString("dimension"));
				vec.add(rs.getString("type"));
				list.add(vec);
			}			
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(rs!=null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}		
	}
	
	
	
	//������ ���ڵ� ����
	public void delete(){
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object obj=e.getSource();
		if(obj==bt_open){
			open();
		}else if(obj==bt_load){
			load();
		}else if(obj==bt_excel){
			loadExcel();
		}else if(obj==bt_del){
		delete();
		}		
	}
	
	
	//���̺� ���� �����Ͱ��� ������ �߻��ϸ�, �� ������ �����ϴ� ������!
	
	public void tableChanged(TableModelEvent e) {
		System.out.println("���ٲ�?");
		//���� �ٲ� ��ġ�� �˷����!
		//�����Ҷ����� ������Ʈ ���� ����ض�
		
		int col=e.getColumn();
		int row=e.getFirstRow();
		TableModel model = (TableModel)e.getSource();
	    String columnName = model.getColumnName(col);
	    Object seq = model.getValueAt(row, 0);
	    Object data = model.getValueAt(row, col);
	    
		System.out.println("����� �ٲٰ� �ִ� ���� "+row+"ĭ�� "+col);
		System.out.println("update hospital set "+columnName+"='"+data+"' where seq='"+seq+"'");
		
		StringBuffer sb=new StringBuffer();
		sb.append("update hospital set "+columnName+"='"+data+"' where seq='"+seq+"'");
		PreparedStatement pstmt=null;
		
		try {
			pstmt=con.prepareStatement(sb.toString());
			pstmt.executeUpdate();
		} catch (SQLException e1) {

			e1.printStackTrace();
		} finally {
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}		
	}
	
	
	public void run() {
		String[] str = sql.toString().split(";");
		PreparedStatement pstmt=null;
		
		for(int i=0;i<str.length;i++){
			System.out.println(str[i]);
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				pstmt=con.prepareStatement(str[i]);
				int result=pstmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			} 
		}
		//������ ����ߴ� StringBuffer����
		sql.delete(0, sql.length());
		
		if(pstmt!=null){
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}JOptionPane.showMessageDialog(this, "������ ����");
	}

	public static void main(String[] args) {
		new LoadMain();
	}

}
