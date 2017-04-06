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



public class LoadMain extends JFrame implements ActionListener, TableModelListener{
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

	public LoadMain() {
		p_north = new JPanel();
		t_path = new JTextField(20);
		bt_open = new JButton("���Ͽ���");
		bt_load = new JButton("�ε��ϱ�");
		bt_excel = new JButton("�����ε�");
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
		//POI HSSFWorkbook �����ϱ� ���ؼ� ���븦 �žҴ�.
		int option=chooser.showOpenDialog(this);
		if(option==JFileChooser.APPROVE_OPTION){
			File file =chooser.getSelectedFile();
			FileInputStream fis=null;
			PreparedStatement pstmt=null;
			try {
				fis=new FileInputStream(file);
				
				HSSFWorkbook book=null;
				book=new HSSFWorkbook(fis);
				HSSFSheet sheet=null;
				sheet=book.getSheet("sheet1");
				DataFormatter df=new DataFormatter();
				String[] values=null;
				
				int total = sheet.getLastRowNum();
				for(int a=1; a<=total;a++){
					HSSFRow row=sheet.getRow(a);
					int cols=row.getLastCellNum();
					values = new String[cols];
					for(int i=0;i<cols;i++){
						HSSFCell cell=row.getCell(i);
						String value=df.formatCellValue(cell);
						values[i]=value;
					}
					StringBuffer sb= new StringBuffer();
					sb.append("insert into hospital(seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values ("+values[0]+", '"+values[1]+"', '"+values[2]+"', '"+values[3]+"', '"+values[4]+"', "+values[5]+", '"+values[6]+"')");
					pstmt=con.prepareStatement(sb.toString());
					int result=pstmt.executeUpdate();
					sb.delete(0, sb.length());
				}JOptionPane.showMessageDialog(this, "������ ����");		
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
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
	

	public static void main(String[] args) {
		new LoadMain();

	}

}
