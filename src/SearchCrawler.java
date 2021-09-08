import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchCrawler extends JFrame {
    //콤보박스의 drop-down 값
    private static final String[] MAX_URLS =
            {"50", "100", "500", "1000"};

    //호스트별로 금지(disallow)된 경로(path) 목록을 저장
    private HashMap disaloowListCache = new HashMap();

    //검색 섹션 GUI 컨트롤
    private JTextField startTextField;
    private JComboBox maxComboBox;
    private JCheckBox limitCheckBox;
    private JTextField logTextField;
    private JTextField searchTextField;
    private JCheckBox caseCheckBox;
    private JButton searchButton;

    //검색 상태 섹션 GUI 컨트롤
    private JLabel crawlingLabel2;
    private JLabel crawledLabel2;
    private JLabel toCrawlLabel2;
    private JProgressBar progressBar;
    private JLabel matchesLabel2;

    //검색 결과 섹션에 결과를 보여주는 테이블
    private JTable table;

    //크롤링이 진행중인지 여부를 나타내는 플래그
    private boolean crawling;

    //검색 결과를 로그 파일에 쓰기 위한 PrintWriter
    private PrintWriter logFileWriter;

    //생서아
    public SearchCrawler() {
        //타이틀 설정
        setTitle("Search Crawler");

        //윈도우 크기 생성
        setSize(600, 600);

        //closing 이벤트 핸들링
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                actionExit();
            }
        });

        //파일 메뉴 설정
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(e -> {
            actionExit();
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        //검색 판넬 설정
        JPanel searchPanel = new JPanel();
        GridBagConstraints constraints;
        GridBagLayout layout = new GridBagLayout();
        searchPanel.setLayout(layout);

        JLabel startLabel = new JLabel("Start URL:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(startLabel, constraints);
        searchPanel.add(startLabel);

        startTextField = new JTextField();
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(startTextField, constraints);
        searchPanel.add(startTextField);

        JLabel maxLabel = new JLabel("Max URLs to Crawl");
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(maxLabel, constraints);
        searchPanel.add(maxLabel);

        maxComboBox = new JComboBox(MAX_URLS);
        maxComboBox.setEnabled(true);
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(maxComboBox, constraints);
        searchPanel.add(maxComboBox);

        limitCheckBox = new JCheckBox("Limit crawling to Start URL site");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 10, 0, 0);
        layout.setConstraints(limitCheckBox, constraints);
        searchPanel.add(limitCheckBox);

        JLabel blankLabel = new JLabel();
        constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(blankLabel, constraints);
        searchPanel.add(blankLabel);

        JLabel logLabel = new JLabel("Matches Log File: ");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(logLabel, constraints);
        searchPanel.add(logLabel);

        String file = System.getProperty("user.dir") + System.getProperty("file.separator") + "crawler.log";
        logTextField = new JTextField(file);
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(logTextField, constraints);
        searchPanel.add(logTextField);

        JLabel searchLabel = new JLabel("Search String : ");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(searchLabel, constraints);
        searchPanel.add(searchLabel);

        searchTextField = new JTextField();
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 0);
        constraints.gridwidth = 2;
        constraints.weightx = 1.0d;
        layout.setConstraints(searchTextField, constraints);
        searchPanel.add(searchTextField);

        caseCheckBox = new JCheckBox("Case Sensitive");
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(caseCheckBox, constraints);
        searchPanel.add(caseCheckBox);

        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionSearch();
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 5, 5);
        layout.setConstraints(searchButton, constraints);
        searchPanel.add(searchButton);

        JSeparator separator = new JSeparator();
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 5, 5);
        layout.setConstraints(separator, constraints);
        searchPanel.add(separator);

        JLabel crawlingLabel1 = new JLabel("Crawling");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(crawlingLabel1, constraints);
        searchPanel.add(crawlingLabel1);

        crawlingLabel2 = new JLabel();
        crawlingLabel2.setFont(crawlingLabel2.getFont().deriveFont(Font.PLAIN));
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(crawlingLabel2, constraints);
        searchPanel.add(crawlingLabel2);

        JLabel crawledLabel1 = new JLabel("Crawled URLs");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(crawledLabel1, constraints);
        searchPanel.add(crawledLabel1);

        crawledLabel2 = new JLabel();
        crawledLabel2.setFont(
                crawledLabel2.getFont().deriveFont(Font.PLAIN)
        );
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(crawledLabel2, constraints);
        searchPanel.add(crawledLabel2);

        JLabel toCrawlLabel1 = new JLabel("URLs to Crawl:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(toCrawlLabel1, constraints);
        searchPanel.add(toCrawlLabel1);
        toCrawlLabel2 = new JLabel();
        toCrawlLabel2.setFont(
                toCrawlLabel2.getFont().deriveFont(Font.PLAIN));
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(toCrawlLabel2, constraints);
        searchPanel.add(toCrawlLabel2);
        JLabel progressLabel = new JLabel("Crawling Progress:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(progressLabel, constraints);
        searchPanel.add(progressLabel);
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setStringPainted(true);
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(progressBar, constraints);
        searchPanel.add(progressBar);
        JLabel matchesLabel1 = new JLabel("Search Matches:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 10, 0);
        layout.setConstraints(matchesLabel1, constraints);
        searchPanel.add(matchesLabel1);

        matchesLabel2 = new JLabel();
        matchesLabel2.setFont(
                matchesLabel2.getFont().deriveFont(Font.PLAIN));
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 10, 5);
        layout.setConstraints(matchesLabel2, constraints);
        searchPanel.add(matchesLabel2);
// Set up matches table.
        table =
                new JTable(new DefaultTableModel(new Object[][]{},
                        new String[]{"URL"}) {
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                });
// Set up Matches panel.
        JPanel matchesPanel = new JPanel();
        matchesPanel.setBorder(
                BorderFactory.createTitledBorder("Matches"));
        matchesPanel.setLayout(new BorderLayout());
        matchesPanel.add(new JScrollPane(table),
                BorderLayout.CENTER);
// Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(searchPanel, BorderLayout.NORTH);
        getContentPane().add(matchesPanel, BorderLayout.CENTER);
    }

    // Exit this program.
    private void actionExit() {
        System.exit(0);
    }

    //검색/정지(Search/Stop) 버튼 클릭
    private void actionSearch() {
        //정지 버튼이 클릭되면 crawling 플래그를 끈다
        if (crawling) {
            crawling = false;
            return;
        }


        ArrayList errorList = new ArrayList();

        //시작 페이지가 입력되었는지 확인(validation)
        String startUrl = startTextField.getText().trim();
        if (startUrl.length() < 1) {
            errorList.add("Missing Start URL.");
        }//시작 페이지 URL의 검증(verification)
        else if (verifyUrl(startUrl) == null) {
            errorList.add("Invalid Start URL.");
        }

        //최대 크롤링할 URL의 개수 확인
        int maxUrls = 0;
        String max = ((String) maxComboBox.getSelectedItem()).trim();
        if (max.length() > 0) {
            try {
                maxUrls = Integer.parseInt(max);
            } catch (NumberFormatException e) {
            }

            if (maxUrls < 1) {
                errorList.add("Invalid Max URLs Value");
            }
        }

        //검색 결과 로그 파일이 입력되었는지 확인
        String logFile = logTextField.getText().trim();
        if (logFile.length() < 1) {
            errorList.add("Missing Matches Log File");
        }

        //검색어가 입력되었는지 확ㅇ니
        String searchString = searchTextField.getText().trim();
        if (searchString.length() < 1) {
            errorList.add("Missing Search String.");
        }

        //에러가 있따면 출력하고 리턴
        if (errorList.size() > 0) {
            //StringBuffer을 사용하는 이유
            //문자열에 대해서 많은 추가 삭제가 있을 경우
            //String 객체보다 속도가 빠르다
            //하지만 적은 횟수의 수정이 있을 경웨는
            //StringBuffer은 결국 toString으로 String 객체로
            //변환해야되기 때문에 많은 자원이 소비된다

            StringBuffer message = new StringBuffer();

            //발생한 에러들을 하나의 메시지로 합친다
            for (int i = 0; i < errorList.size(); i++) {
                message.append(errorList.get(i));
                if (i + 1 < errorList.size()) {
                    message.append("\n");
                }
            }

            showError(message.toString());
            return;
        }

        //싲가 페이지 URL에서 "www" 문자열 제거
        startUrl = removeWwwFromUrl(startUrl);

        //검색 크롤러 시작
        search(logFile, startUrl, maxUrls, searchString);
    }

    private void search(final String logFile, final String startUrl, final int maxUrls, final String searchString) {
        //새 스레드를 생성하여 검색 수행
        Thread thread = new Thread(new Runnable() {
            public void run() {
                //크롤링이 수행되는 동안 모리시계로 커서를 바꾼다.
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                //검색 관련 컨트롤 비활성화
                startTextField.setEnabled(false);
                maxComboBox.setEnabled(false);
                limitCheckBox.setEnabled(false);
                logTextField.setEnabled(false);
                searchTextField.setEnabled(false);
                caseCheckBox.setEnabled(false);

                //검색 버튼의 텍스트를 "정지(STOP)로 설정"
                searchButton.setText("Stop");

                //검색 상태 재설정
                table.setModel(new DefaultTableModel(new Object[][]{}, new String[]{"URL"}) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                });
                updateStats(startUrl, 0, 0, maxUrls);

                //검색 결과 로그 파일을 연다
                try {
                    logFileWriter = new PrintWriter(new FileWriter(logFile));
                } catch (Exception e) {
                    showError("Unable to open matches log file.");
                    return;
                }

                //crawling 플래그를 켠다
                crawling = true;

                //실제로 크롤링을 수행하는 메소드
                crawl(startUrl, maxUrls, limitCheckBox.isSelected(), searchString, caseCheckBox.isSelected());

                //crawling 플래그를 끈다.
                crawling = false;

                //검색 결과 로그 파일을 닫는다.
                try {
                    logFileWriter.close();
                } catch (Exception e) {
                    showError("Unable to close matches log file");
                }

                //검색이 완료되었음을 표시
                crawlingLabel2.setText("Done");

                //검색 관련 컨트롤 활성화
                startTextField.setEnabled(true);
                maxComboBox.setEnabled(true);
                limitCheckBox.setEnabled(true);
                logTextField.setEnabled(true);
                searchTextField.setEnabled(true);
                caseCheckBox.setEnabled(true);

                //검색 버튼의 텍스트를 "검색(Search)"으로 실행
                searchButton.setText("Search");

                //기본 커서로 되돌린다.
                setCursor(Cursor.getDefaultCursor());

                //검색 결과가 하나도 나오지 않았음ㅇ르 알린다.
                if (table.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(SearchCrawler.this, "Your Search String was not found.",
                            "Search String Not Found", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        thread.start();
    }

    //에러출력
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    //검색 상태 갱신
    private void updateStats(String crawling, int crawled, int toCrawl, int maxUrls) {
        crawlingLabel2.setText(crawling);
        crawledLabel2.setText("" + crawled);
        toCrawlLabel2.setText("" + toCrawl);

        //프로그래스 바 갱신
        if (maxUrls == -1) {
            progressBar.setMaximum(crawled + toCrawl);
        } else {
            progressBar.setMaximum(maxUrls);
        }

        progressBar.setValue(crawled);

        matchesLabel2.setText("" + table.getRowCount());
    }

    //검색 결과를 테이블과 로그 파일에 추가
    private void addMatch(String url) {
        //검색 결과 URL을 테이블에 추가
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{url});

        //검색 겨로가 URL을 로그 파일에 추가
        try {
            logFileWriter.println(url);
        } catch (Exception e) {
            showError("Unable to log match");
        }
    }

    //URL 포맷 검중
    private URL verifyUrl(String url) {
        //HTTP URL만 허용
        if (!url.toLowerCase().startsWith("https://"))
            return null;

        //URL 포맷 검증
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        return verifiedUrl;
    }

    //해당 URL에 대해 로봇의 접근이 허락되는지 검사
    private boolean isRobotAllowed(URL urlToCheck) {
        String host = urlToCheck.getHost().toLowerCase();

        //해당 호소트에 대한 금지 경로 목록이 이미 저장되어 있는지 검사
        ArrayList disallowList = (ArrayList) disaloowListCache.get(host);

        //금지 경로 목록이 없다면 다운로드하여 저장
        if (disallowList == null) {
            disallowList = new ArrayList();
        }

        try {
            URL robotsFileUrl = new URL("https://" + host + "/robots.txt");

            //로봇 파일을 연다
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(robotsFileUrl.openStream()));

            //로봇 파일을 읽어서 금지 경로 목록을 만든다.
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("Disallow:") == 0) {
                    String disallowPath = line.substring("Disallow:".length());

                    //현재 금지 경로에 주석이 포함되어 있다면 제거한다.
                    int commentIndex = disallowPath.indexOf("#");
                    if (commentIndex != -1) {
                        disallowPath = disallowPath.substring(0, commentIndex);
                    }

                    //공백 문자(whilespace charactoers)제거
                    disallowPath = disallowPath.trim();

                    //금지 목록에 경로 추가
                    disallowList.add(disallowPath);
                }
            }

            //호스트별 금지 경로 목록에 저장
            disaloowListCache.put(host, disallowList);
        } catch (Exception e) {
            //로봇 파일이 없을 때 예외가 발생하므로 접근이 허락된 것으로 가정한다.
            return true;
        }

        //금지 경로 목록을 순회하면서 해당 URL에 대한 접근이 금지되었는지 검사
        String file = urlToCheck.getFile();
        for (int i = 0; i < disallowList.size(); i++) {
            String disallow = (String) disallowList.get(i);
            if (file.startsWith(disallow)) {
                return false;
            }
        }

        return true;
    }

    //해당 URL에 대한 페이지를 다운로드
    private String downloadPage(URL pageURL) {
        try {
            //해당 url로 연결
            BufferedReader reader = new BufferedReader(new InputStreamReader(pageURL.openStream()));

            //버퍼로 페이지를 읽어들임
            String line;
            StringBuffer pageBuffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                pageBuffer.append(line);
            }

            return pageBuffer.toString();
        } catch (Exception e) {
        }

        return null;
    }

    //URL에서 "www"문자열 제거
    private String removeWwwFromUrl(String url){
        int index = url.indexOf("://www.");
        if(index != -1){
            return url.substring(0,index+3) + url.substring(index+7);
        }
        return (url);
    }

    //페이지 내용을 파싱하여 링크 정보를 가져온다
    private ArrayList retriveLinks(URL pageUrl, String pageContents, HashSet crawledList,boolean limitHost){
        //링크를 나타내는 정규식(regular expression) 패턴 명세
        Pattern p = Pattern.compile("<a\\s+href\\s*=\\s*\"?(.*?)[\"|>]",Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(pageContents);

        //링크 리스트를 만든다.
        ArrayList linkList = new ArrayList();
        while(m.find()){
            String link = m.group(1).trim();

            //빈 링크 무시
            if(link.length() < 1){
                continue;
            }

            //페이지 앵커만으로 구성된 링크 무시
            if(link.charAt(0) == '#'){
                continue;
            }

            //메일 링크 무시
            if(link.indexOf("mailto:")!=-1){
                continue;
            }

            //자바 스크립트 링크 무시
            if(link.toLowerCase().indexOf("javascript")!=-1){
                continue;
            }

            //절대 경로 및 상대 경로를 완전한 링크로 바꾼다
            if(link.indexOf("://")==-1){
                //URL 절대 경로 처리
                if(link.charAt(0) == '/'){
                    link = "https://" + pageUrl.getHost() + link;

                    //URL 상대 경로 처리
                }else{
                    String file = pageUrl.getFile();
                    if(file.indexOf('/')==-1){
                        link = "https://" + pageUrl.getHost() + "/" + link;
                    }else{
                        String path = file.substring(0,file.lastIndexOf('/') + 1);
                        link = "https://" + pageUrl.getHost() + path + link;
                    }
                }
            }

            //링크에서 앵커를 제거
            int index = link.indexOf('#');
            if(index != -1){
                link = link.substring(0,index);
            }

            //링크에서 "www"문자열 제거
            link = removeWwwFromUrl(link);

            //링크를 검증하고 유효하지 않다면 무시한다.
            URL verifiedLink = verifyUrl(link);
            if(verifiedLink == null){
                continue;
            }

            //검색 범위를 시작 페이지로 제한하는 경우
            if(limitHost && !pageUrl.getHost().toLowerCase().equals(verifiedLink.getHost().toLowerCase())){
                continue;
            }

            //이미 크롤링된 링크라면 ㅁ쉬
            if(crawledList.contains(link)){
                continue;
            }

            //리스트에 링크를 추가
            linkList.add(link);
        }
        return (linkList);
    }


    //해당 페이지 내용에 사용자가 입력한 검색어가 있는지 검사
    private boolean searchStringMatches(String pageContents,String searchString, boolean caseSensitive){
        String searchContents = pageContents;

        //대소문자를 구분하지 않는다면 비교를 위해 페이지 내용을 전부 소문자로 변경
        if(!caseSensitive){
            searchContents = pageContents.toLowerCase();
        }

        //검색어를 각각의 단어들로 나눈ㄴ다.
        Pattern p = Pattern.compile("[\\s]+");
        String[] terms = p.split(searchString);

        //각 단어에 해당하는 검색 결과가 있는지 검사
        for(int i=0;i< terms.length; i++){
            if(caseSensitive){
                if(searchContents.indexOf(terms[i])==-1){
                    return false;
                }
            }else{
                if(searchContents.indexOf(terms[i].toLowerCase())==-1){
                    return false;
                }
            }
        }

        return true;
    }

    public void crawl(String startUrl,int maxUrls,boolean limitHost,String searchString,boolean caseSensitive) {
        //크롤링 작업에 필요한 리스트 설정

        //LinkedHashSet을 사용하는 이유?
        LinkedHashSet toCrawlList = new LinkedHashSet();

        //thread-safe한 ConcurrentHashMap을 가지고 set으로 설정함
        //여기서 key값은 String이 되고 Value는 의미 없는 값이 됨
        Set<String> crawledList = ConcurrentHashMap.<String>newKeySet();




        //크롤링할 리스트에 시작 페이지 URL추가
        toCrawlList.add(startUrl);

        //크롤링할 리스트를 순회하면서 크롤링 수행
        while (crawling && toCrawlList.size() > 0) {
            //크롤링할 최대 URL 개수에 도달했는지 검사
            if (maxUrls != -1) {
                if (crawledList.size() == maxUrls) {
                    break;
                }
            }

            //이것은 약한 연결이기 때문에 멀티 쓰레딩 영역에서 사용할 수 없음
            //크롤링할 리스트에서 현재 처리할 URL을 얻음
            String url = (String) toCrawlList.iterator().next();

            //크롤링할 리스트에서 URL제거
            toCrawlList.remove(url);

            //스트링 url을 URL객체로 반환
            URL verifiedUrl = verifyUrl(url);

            //URL이 금지된 경로라면 처리하지 않는다.(양심적인 크롤러를 만들자!)
            if (!isRobotAllowed(verifiedUrl)) {
                continue;
            }

            //검색 상태 갱신
            updateStats(url, crawledList.size(), toCrawlList.size(), maxUrls);

            //URL을 크롤링된 목록에 추가
            crawledList.add(url);

            //URL이 가리키는 페이지를 다운로드
            String pageContents = downloadPage(verifiedUrl);

            //페이지의 다운로드가 성공했다면, 해당 페이지의 모든 링크를 뽑아내고
            //페이지의 내용이 검색어를 포함하는 검사한다
            if (pageContents != null && pageContents.length() > 0) {
                //페이지에서 유효한 링크 목록을 뽑ㄴ드ㅏ.
                ArrayList links = retriveLinks(verifiedUrl, pageContents, crawledList, limitHost);

                //크롤링할 리스트에 링크 추가
                toCrawlList.addAll(links);

                //페이지에 검색어가 있다면 검색 결과에 추가
                if (searchStringMatches(pageContents, searchString, caseSensitive)) {
                    addMatch(url);
                }
            }

            //검색 상태 갱신
            updateStats(url, crawledList.size(), toCrawlList.size(), maxUrls);
        }
    }


    //검색 크롤러의 메인 메소드
    public static void main(String[] args){
        SearchCrawler crawler = new SearchCrawler();
        crawler.show();
    }
}
