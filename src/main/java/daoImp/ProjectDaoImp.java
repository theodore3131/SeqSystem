package daoImp;


import com.opensymphony.xwork2.ActionContext;
import dao.CatalogDao;
import dao.DAO;
import dao.DocumentDao;
import dao.ProjectDao;
import entity.CatalogEntity;
import entity.ProjectEntity;
import entity.ShowOrgProjectEntity;
import entity.UserEntity;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

public class ProjectDaoImp extends DAO<ProjectEntity> implements ProjectDao {

    public boolean save(ProjectEntity p) {
        String sql0 = "insert into PROJECT(NAME,DATE,DOCUMENT_NAME,STATE,INTRO) values(?,?,?,?,?)";
        String sql1 = "insert into PROJECT(NAME,DATE,DOCUMENT_NAME,STATE,ID_ORGANIZATION,INTRO) values(?,?,?,?,?,?)";
        String sql2 = "select ID_ORGANIZATION from ORGANIZATION where NAME = ?";
        String sql3 = "insert into PROJECT_MEMBER(ID_PROJECT,ID_USER,RANK) values(?,?,?)";

//      use getTime() instead of getDate() to get current date.
        Date createDate = new Date(new java.util.Date().getTime());

        Timestamp time = new Timestamp(new java.util.Date().getTime());

        int ID_Org = 0;


//      project Name and Document Name cannot be null
        if (p.getName().length()==0||p.getDocument_Name().length()==0){
            return false;
        }

        String orgName = p.getOrgName();
        int len = orgName.length();
//      if org is not provided
        if (len==0){
            ID_Org = 0;
        }

        else  {
            try {
                ID_Org = getForValueThrowsExp(sql2, p.getOrgName());
            } catch (Exception e) {
                return false;
            }
        }

        UserEntity user = (UserEntity)ActionContext.getContext().getSession().get("user");
        int ID_User = user.getId_user();

        try{
//          新增项目，同时获取自增项目ID
            int Id_Project = 0;
            if (ID_Org>0) {
                Id_Project = insert(sql1, p.getName(), createDate, p.getDocument_Name(), 1, ID_Org, p.getIntro());
            }
            else {
                Id_Project = insert(sql0, p.getName(), createDate, p.getDocument_Name(), 1, p.getIntro());
            }

//            新建文档
            DocumentDao documentDao = new DocumentDaoImp();
            documentDao.create(Id_Project,1,time,ID_User);
//          set PM of one Project
            updateThrowException(sql3,Id_Project,ID_User,3);

            return true;
        }catch (Exception e){
            return false;
        }
    }


    public UserEntity getPM(ProjectEntity p){
        String sql="select ID_USER from VIEW_projectMember where RANK = 3 and ID_PROJECT = ?";
        int ID_user = getForValue(sql,p.getId_Project());
        UserDaoImp userDao = new UserDaoImp();
        UserEntity user = userDao.getOne1(ID_user);
        return user;
    }

    @Override
    public List<UserEntity> getMember(ProjectEntity p) {
        String sql="select USER.ID_USER, USER.NAME, MAIL, TEL, RANK from USER, VIEW_projectMember where USER.ID_USER=VIEW_projectMember.ID_USER and ID_PROJECT=? order by rank";
        UserDaoImp userDao = new UserDaoImp();
        List<UserEntity> members = userDao.getForList(sql,p.getId_Project());
        return members;
    }

    @Override
    public List<UserEntity> getMatched(ProjectEntity p, String name) {
        name = "%"+name+"%";
        String sql="select USER.ID_USER, USER.NAME, RANK from USER, VIEW_projectMember where USER.ID_USER=VIEW_projectMember.ID_USER and VIEW_projectMember.ID_PROJECT = ? and USER.NAME LIKE ?";
        UserDaoImp userDao = new UserDaoImp();
        List<UserEntity> members = userDao.getForList(sql,p.getId_Project(),name);
        return members;
    }

    @Override
    public boolean alterPM(int idUser, int idProject) {
//        判断被转移人是否在组内
        String sql = "select count(*) from PROJECT_MEMBER where ID_PROJECT = ? and ID_USER = ?";
        if (Integer.valueOf(getForValue(sql,idProject,idUser).toString())<1){
            return false;
        }
        else {
            try {
                String sql1="update PROJECT_MEMBER set RANK=5 where ID_PROJECT = ? and RANK = 3";
                update(sql1,idProject);
                String sql2 = "update PROJECT_MEMBER set RANK=3 where ID_PROJECT = ? and ID_USER = ?";
                updateThrowException(sql2, idProject, idUser);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Override
    public void setVPM(int idUser,int idProject) {
        String sql="update PROJECT_MEMBER set RANK=4 where ID_USER = ? and ID_PROJECT = ?";
        update(sql,idUser,idProject);
    }

    @Override
    public void dismissVPM(int idUser,int idProject) {
        String sql="update PROJECT_MEMBER set RANK=5 where ID_USER = ? and ID_PROJECT = ?";
        update(sql,idUser,idProject);
    }

    @Override
    public void deleteMember(int idUser,int idProject) {
        String sql="delete from PROJECT_MEMBER where ID_USER = ? and ID_PROJECT = ?";
        update(sql,idUser,idProject);
    }

    @Override
    public boolean inviteMember(int idUser,int idProject,String content) {

        String sql1 = "select count(*) from PROJECT_MEMBER where ID_PROJECT = ? and ID_USER = ?";

        if (Integer.valueOf(getForValue(sql1,idProject,idUser).toString())==1) {
            return false;
        }

        else {
            String sql = "insert into PROJECT_APPLY(ID_PROJECT,ID_USER,DATE,MESSAGE) VALUES (?,?,?,?)";

            Timestamp time = new Timestamp(new java.util.Date().getTime());

            try {
                updateThrowException(sql, idProject, idUser, time, content);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }


    @Override
    public void end(int idProject) {
        String sql = "update PROJECT set STATE = 0 where ID_PROJECT = ?";
        update(sql,idProject);
    }

    @Override
    public int getRank(int idProject, int idUser) {
        String sql = "select RANK from PROJECT_MEMBER where ID_PROJECT = ? and ID_USER = ?";
        int rank=Integer.valueOf(getForValue(sql,idProject,idUser).toString());
        return rank;
    }

    @Override
    public ProjectEntity getOne(int id) {
        String sql="select * from VIEW_projectINFO where ID_PROJECT=?";
        ProjectEntity project1 = get(sql,id);
        return project1;
    }

    @Override
    public List<ProjectEntity> getAll(int state,int id) {
        String sql="select * from VIEW_projectINFO where STATE = ? and ID_USER = ? ";
        List<ProjectEntity> project = getForList(sql,state,id);
        return project;
    }
//查询项目名称
    @Override
    public String findName(int id_Project) {
        String sql = "select NAME from PROJECT where ID_PROJECT = ?";
        String name = getForValue(sql,id_Project);
        System.out.println("orgName:"+name);
        return name;
    }
    //查询项目组长名
    @Override
    public String findAdminName(int id_Project){
        System.out.println("项目ID:(ProjectDaoImp.java203)"+id_Project);
        String sql = "select ID_USER from PROJECT_MEMBER where ID_PROJECT = ? and RANK = 3";
        int id_admin = getForValue(sql,id_Project);
        String sql0 = "select NAME from USER where ID_USER = ?";
        String name = getForValue(sql0,id_admin);
        return name;
    }

    @Override
    public boolean copyAll(int id_document,int new_idDocument, int version) {
        System.out.println("start");
        CatalogDao catalogDao=new CatalogDaoImp();
        try {
            List<CatalogEntity> catalogEntityList=catalogDao.getAllByDocument(id_document);
            CatalogEntity catalogEntity;
            for (int i = 0; i < catalogEntityList.size(); i++) {
                catalogEntity=catalogEntityList.get(i);
                catalogDao.insert(catalogEntity.getId_template(),new_idDocument,catalogEntity.getTitle(),catalogEntity.getFirst_index(),catalogEntity.getSecond_index(),catalogEntity.getThird_index(),catalogEntity.getFourth_index());
            }
        }
        catch (Exception e){
            System.out.println("exception:"+e);
            String sql="delete from CATALOG where id_document=? ";
            update(sql,id_document,version+1);
            return false;
        }
        return true;
    }


}
