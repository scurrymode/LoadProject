/*파일과 관련된 작업을 도와주는 재사용성있는 클래스 정의한다*/

package util.file;

public class FileUtil {
	/*넘겨받는 경로에서 확장자 구하기*/
	public static String getExt(String path){
		int last = path.lastIndexOf(".");
		
		return path.substring(last+1, path.length());
	}
}
