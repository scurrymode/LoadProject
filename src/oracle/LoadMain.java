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
	
	//윈도우창이 열리면 미리 접속을 확보해놓자!
	DBManager manager;
	Connection con;
	
	Vector list;
	Vector columnName;
	
	Thread thread; //엑셀 등록시 사용될 쓰레드 쓰는 이유? 데이터양이 너무 많거나, 네트워크 상태가 좋지않을 경우, insert가 while문 속도를 못따라갈 수 있기에 안정성을 위해서
	//일부러 시간 지연을 일으킨다.
	
	//엑셀 파일에 의해 생성된 쿼리문을 쓰레드가 사용할 수 있는 상태로 저장해놓자!
	StringBuffer sql = new StringBuffer();

	public LoadMain() {
		p_north = new JPanel();
		t_path = new JTextField(20);
		bt_open = new JButton("csv파일열기");
		bt_load = new JButton("로드하기");
		bt_excel = new JButton("excel로드");
		bt_del = new JButton("삭제하기");
		
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
		
		//버튼에 리스너 붙이기!
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_excel.addActionListener(this);
		bt_del.addActionListener(this);		
		
		//윈도우와 리스너 연결
		//윈도우리스너는 오버라이딩이 넘 많고, 윈도우어댑터 extends하려했더니 이미 JFrame하고 있어서 내부익명으로
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//데이터베이스 자원해제
				manager.disConnect(con);
				//프로세스 종료
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
	
	//파일 탐색기 띄우기
	public void open(){
		int result = chooser.showOpenDialog(this);
		
		//열기를 누르면.. 목적파일의 스트림을 생성하자!
		if(result==JFileChooser.APPROVE_OPTION){
			
						
			//유저가 선택한 파일
			File file = chooser.getSelectedFile();
			
			//csv파일 아니면 뱉어내기!
			String name = file.getName();
			
			String ext=FileUtil.getExt(name);
			
			if(!ext.equals("csv")){
				JOptionPane.showMessageDialog(this, "csv 파일만 선택해주세요");
				return;
			}
			
				t_path.setText(file.getAbsolutePath());
				
				try {
					//스트림만 만들고 말자!
					reader = new FileReader(file);
					buffr = new BufferedReader(reader);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} 		
				
			
		}
	}
	
	//CSV->Oracle로 데이터 이전(migration)하기
	public void load(){
		//버퍼스트림을 이용하여 csv의 데이터를 한줄씩 읽어들여 insert시키자! 레코드가 없을때까지 그런데 while문으로 돌리면 너무 빠르므로 Thread로 sleep주자!
		String data;
		StringBuffer sb = new StringBuffer();
		PreparedStatement pstmt=null;
		
		try {
			while(true){
				data=buffr.readLine();
				
				if(data==null) break;
				
				String[] value=data.split(",");
				
				//seq줄을 제외하고 insert하겠다.
				if(!value[0].equals("seq")){
					sb.append("insert into hospital(seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values ("+value[0]+", '"+value[1]+"', '"+value[2]+"', '"+value[3]+"', '"+value[4]+"', "+value[5]+", '"+value[6]+"')");
					
					pstmt=con.prepareStatement(sb.toString());
					int result=pstmt.executeUpdate(); //쿼리수행
					//기존에 누적된 StringBuffer 데이터를 모두 지우기
					sb.delete(0, sb.length());
					
				}else{
					System.out.println("난 1줄이므로 제외");
				}
			}
			JOptionPane.showMessageDialog(this, "Migration완료!!");
			
			//JTable 나오게 처리!!
			getList();
			//테이블 모델로 테이블을 만들고!
			table.setModel(new MyModel(list, columnName));
			
			//테이블 모델과 리스너와의 연결
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
	
	//엑셀파일 읽어서 db에 마이그레이션 하기!!
	//javaSE 엑셀제어 라이브러리가 없다!
	//open Source 공개소프트웨어
	//copyright <---> copyleft(아파치 단체)
	//POI 라이브러리! http://apache.org
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
		
		//POI HSSFWorkbook 실행하기 위해서 빨대를 꼽았다.
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
				 첫번째 row는 데이터가 아닌 컬럼 정보이므로,
				 이 정보들을 추출하여 insert into table(***)에 넣자
				 -------------------------------------------*/
				
				HSSFRow firstRow=sheet.getRow(sheet.getFirstRowNum());
				//row를 얻었으니 컬럼을 분석한다
				//int last = firstRow.getLastCellNum();//마지막 cell 번호
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
				
				//데이터 추출
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
				//모든게 끝났으니, 편안하게 쓰레드에게 일 시키자!
				thread = new Thread(this);//this를 붙임으로써, Runnable인 클래스의 run을 수행한다.
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
	
	//모든 레코드 가져오기!!
	public void getList(){
		String sql = "select * from hospital order by seq asc";
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			ResultSetMetaData meta = rs.getMetaData();
			
			//컬럼이름도 벡터에 넣자~!
			int count = meta.getColumnCount();
			
			columnName=new Vector();
			
			for(int i=0;i<count;i++){
				columnName.add(meta.getColumnName(i+1));
			}
			
			list = new Vector<Vector>(); //이차원 벡터
			
			while(rs.next()){
				Vector vec = new Vector(); //레코드 1건 담을 거임
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
	
	
	
	//선택한 레코드 삭제
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
	
	
	//테이블 모델의 데이터값에 변경이 발생하면, 그 찰나를 감지하는 리스너!
	
	public void tableChanged(TableModelEvent e) {
		System.out.println("나바꿈?");
		//내가 바꾼 위치를 알려줘라!
		//수정할때마다 업데이트 문을 출력해라
		
		int col=e.getColumn();
		int row=e.getFirstRow();
		TableModel model = (TableModel)e.getSource();
	    String columnName = model.getColumnName(col);
	    Object seq = model.getValueAt(row, 0);
	    Object data = model.getValueAt(row, col);
	    
		System.out.println("당신이 바꾸고 있는 줄은 "+row+"칸은 "+col);
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
		//기존에 사용했던 StringBuffer비우기
		sql.delete(0, sql.length());
		
		if(pstmt!=null){
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}JOptionPane.showMessageDialog(this, "엑셀도 성공");
	}

	public static void main(String[] args) {
		new LoadMain();
	}

}
